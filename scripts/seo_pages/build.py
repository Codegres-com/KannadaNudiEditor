#!/usr/bin/env python3
"""Generate the static SEO landing pages and sitemap for KannadaNudiWeb.

    python3 scripts/seo_pages/build.py

* Writes KannadaNudiWeb/wwwroot/<slug>/index.html for every page in pages_*.py.
* Refreshes the header nav and footer links of the hand-written landing pages so every
  page links to every other one.
* Regenerates wwwroot/sitemap.xml.
* Verifies every keystroke example against js/nudiTypeBox.js and the converter samples
  against js/nudiConverter.js (needs Node.js), so the pages never show wrong keys.
"""
from __future__ import annotations

import datetime as dt
import html
import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import common  # noqa: E402
import pages_charts  # noqa: E402
import pages_software  # noqa: E402
import pages_tools  # noqa: E402
import pages_typing  # noqa: E402

WWW = HERE.parent.parent / "KannadaNudiWeb" / "wwwroot"

GENERATED = pages_typing.PAGES + pages_charts.PAGES + pages_software.PAGES + pages_tools.PAGES

# Hand-written pages: (slug, sitemap priority). Their nav and footer are refreshed.
HAND_WRITTEN = [
    ("kannada-typing", "0.9"),
    ("baraha-kannada-typing", "0.9"),
    ("nudi-to-unicode", "0.9"),
    ("kannada-speech-to-text", "0.8"),
    ("download", "0.8"),
]
PRIORITY = {
    "nudi-ascii-to-unicode": "0.9",
    "kannada-nudi-typing-keyboard-chart": "0.9",
    "how-to-type-in-nudi-kannada": "0.9",
}

NODE_CHECK = r"""
const path = require('path');
const fs = require('fs');
const www = process.argv[1];
const { translate } = require(path.join(www, 'js/nudiTypeBox.js'));
const C = require(path.join(www, 'js/nudiConverter.js'));
const r = f => JSON.parse(fs.readFileSync(path.join(www, 'Resources', f), 'utf8'));
const conv = C.fromJson(r('AsciiToUnicodeMapping.json'), r('UnicodeToAsciiMapping.json'));
const input = JSON.parse(fs.readFileSync(0, 'utf8'));
function type(keys) {
  const st = { buf: '' }; let out = '';
  for (const k of keys) { const [t, del] = translate(st, k); out = out.slice(0, out.length - del) + t; }
  return out;
}
const bad = input.typed.filter(([k, want]) => type(k) !== want).map(([k, want]) => [k, want, type(k)]);
const ascii = conv.unicodeToAscii(input.u2aSample);
const back = conv.asciiToUnicode(ascii);
process.stdout.write(JSON.stringify({ bad, ascii, roundTrip: back === input.u2aSample, back }));
"""


def verify_with_node() -> str:
    pairs = sorted(set(common.TYPED_EXAMPLES))
    payload = json.dumps({"typed": pairs, "u2aSample": pages_tools.U2A_SAMPLE})
    res = subprocess.run(["node", "-e", NODE_CHECK, str(WWW)], input=payload, capture_output=True,
                         text=True, check=True)
    out = json.loads(res.stdout)
    if out["bad"]:
        for keys, want, got in out["bad"]:
            print(f"  WRONG KEYS: {keys!r} gives {got!r}, page says {want!r}", file=sys.stderr)
        sys.exit(f"{len(out['bad'])} keystroke example(s) are wrong")
    if not out["roundTrip"]:
        sys.exit(f"Converter sample does not round-trip: {out['back']!r}")
    print(f"Verified {len(pairs)} keystroke examples and the converter sample.")
    return out["ascii"]


def patch_hand_written(slug: str) -> None:
    p = WWW / slug / "index.html"
    s = p.read_text(encoding="utf-8")
    s, n1 = re.subn(r'(<nav class="site-nav" aria-label="Main">\n).*?(\n\s*</nav>)',
                    lambda m: m.group(1) + common.nav_html(f"/{slug}/") + m.group(2), s, count=1, flags=re.S)
    s, n2 = re.subn(r'(<footer class="site-footer">.*?<ul>\n).*?(\n\s*</ul>)',
                    lambda m: m.group(1) + common.footer_items_html() + m.group(2), s, count=1, flags=re.S)
    if n1 != 1 or n2 != 1:
        sys.exit(f"Could not patch nav/footer in {p}")
    p.write_text(s, encoding="utf-8")


def write_sitemap(today: str) -> None:
    urls = [("", "1.0")] + HAND_WRITTEN + [(p["slug"], PRIORITY.get(p["slug"], "0.8")) for p in GENERATED]
    seen = set()
    rows = []
    for slug, prio in urls:
        if slug in seen:
            sys.exit(f"Duplicate page slug: {slug}")
        seen.add(slug)
        loc = f"{common.SITE}/{slug + '/' if slug else ''}"
        rows.append(f"  <url>\n    <loc>{loc}</loc>\n    <lastmod>{today}</lastmod>\n"
                    f"    <priority>{prio}</priority>\n  </url>")
    xml = ('<?xml version="1.0" encoding="UTF-8"?>\n'
           '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n' + "\n".join(rows) + "\n</urlset>\n")
    (WWW / "sitemap.xml").write_text(xml, encoding="utf-8")
    print(f"Wrote sitemap.xml with {len(rows)} URLs.")


def main() -> None:
    today = dt.date.today().isoformat()
    ascii_sample = verify_with_node()

    footer_slugs = {u.strip("/") for u, _ in common.FOOTER}
    for page in GENERATED:
        if page["slug"] not in footer_slugs:
            sys.exit(f"{page['slug']} is missing from common.FOOTER")
        out = common.render(page).replace("__A2U_SAMPLE__", html.escape(ascii_sample))
        dest = WWW / page["slug"] / "index.html"
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(out, encoding="utf-8")
        print(f"Wrote /{page['slug']}/")

    for slug, _ in HAND_WRITTEN:
        patch_hand_written(slug)
    print(f"Refreshed nav and footer in {len(HAND_WRITTEN)} hand-written pages.")

    write_sitemap(today)


if __name__ == "__main__":
    main()
