"""Shared building blocks for the static SEO landing pages in KannadaNudiWeb/wwwroot.

Every page uses the same header, footer and JSON-LD skeleton as the original hand-written
landing pages (/kannada-typing/, /nudi-to-unicode/, ...), and css/landing.css for styling.
"""
from __future__ import annotations

import html
import json

SITE = "https://nudiweb.com"
SITE_NAME = "Kannada Nudi Editor"

# Header navigation (kept short: it collapses below 760px).
NAV = [
    ("/kannada-typing/", "Kannada Typing"),
    ("/kannada-nudi-typing-keyboard-chart/", "Key Chart"),
    ("/baraha-kannada-typing/", "Baraha Typing"),
    ("/nudi-ascii-to-unicode/", "Nudi Converter"),
    ("/kannada-speech-to-text/", "Speech to Text"),
    ("/download/", "Download"),
]

# Footer: every landing page, so each one is reachable from all others.
FOOTER = [
    ("/", "Online Kannada Editor"),
    ("/kannada-typing/", "Kannada Typing (Nudi)"),
    ("/how-to-type-in-nudi-kannada/", "How to Type in Nudi"),
    ("/kannada-nudi-typing-keyboard-chart/", "Nudi Keyboard Chart"),
    ("/kannada-nudi-typing-letters-in-english/", "Nudi Letters in English"),
    ("/type-kannada-using-english-keyboard/", "Kannada on English Keyboard"),
    ("/learn-nudi-kannada-typing/", "Learn Nudi Typing"),
    ("/how-to-type-ksha-in-kannada-nudi/", "Type ಕ್ಷ & Special Letters"),
    ("/how-to-type-karnataka-in-nudi/", "Type Karnataka in Nudi"),
    ("/nudi-kannada-typing-in-ms-word/", "Nudi Typing in MS Word"),
    ("/baraha-kannada-typing/", "Baraha Typing"),
    ("/nudi-ascii-to-unicode/", "Nudi ASCII to Unicode"),
    ("/nudi-to-unicode/", "Nudi to Unicode (Word files)"),
    ("/nudi-6-to-nudi-4-converter/", "Nudi 6 to Nudi 4 Converter"),
    ("/kannada-speech-to-text/", "Speech to Text"),
    ("/kannada-nudi-app-for-pc/", "Kannada Nudi for PC"),
    ("/kannada-typing-software-for-windows-11/", "Kannada Typing on Windows 11"),
    ("/download/", "Download"),
]

STORE_WINDOWS = "https://apps.microsoft.com/detail/9mxxjp296jlq"
STORE_PLAY = "https://play.google.com/store/apps/details?id=com.kannadanudi.keyboard"
STORE_APPLE = "https://apps.apple.com/us/app/kannada-nudi-editor/id6758589037"
STORE_SNAP = "https://snapcraft.io/kannadanudi"

esc = html.escape

# ---------------------------------------------------------------------------
# Nudi / KGP layout (mirrors Services/KannadaKeyboardEngine.cs)
# ---------------------------------------------------------------------------
VOWELS = [("ಅ", "a"), ("ಆ", "A"), ("ಇ", "i"), ("ಈ", "I"), ("ಉ", "u"), ("ಊ", "U"), ("ಋ", "R"),
          ("ಎ", "e"), ("ಏ", "E"), ("ಐ", "Y"), ("ಒ", "o"), ("ಓ", "O"), ("ಔ", "V"),
          ("ಅಂ", "aM"), ("ಅಃ", "aH")]
MATRAS = [("", "a"), ("ಾ", "A"), ("ಿ", "i"), ("ೀ", "I"), ("ು", "u"), ("ೂ", "U"), ("ೃ", "R"),
          ("ೆ", "e"), ("ೇ", "E"), ("ೈ", "Y"), ("ೊ", "o"), ("ೋ", "O"), ("ೌ", "V"),
          ("ಂ", "M"), ("ಃ", "H")]
VARGAS = [
    [("ಕ", "k"), ("ಖ", "K"), ("ಗ", "g"), ("ಘ", "G"), ("ಙ", "Z")],
    [("ಚ", "c"), ("ಛ", "C"), ("ಜ", "j"), ("ಝ", "J"), ("ಞ", "z")],
    [("ಟ", "q"), ("ಠ", "Q"), ("ಡ", "w"), ("ಢ", "W"), ("ಣ", "N")],
    [("ತ", "t"), ("ಥ", "T"), ("ದ", "d"), ("ಧ", "D"), ("ನ", "n")],
    [("ಪ", "p"), ("ಫ", "P"), ("ಬ", "b"), ("ಭ", "B"), ("ಮ", "m")],
]
AVARGIYA = [("ಯ", "y"), ("ರ", "r"), ("ಲ", "l"), ("ವ", "v"), ("ಶ", "S"), ("ಷ", "x"),
            ("ಸ", "s"), ("ಹ", "h"), ("ಳ", "L")]
SPECIAL = [("ಱ", "rX"), ("ೞ", "LX"), ("ೠ", "RX"), ("ಜ಼", "jX"), ("ಫ಼", "PX")]
CONSONANTS = [c for row in VARGAS for c in row if c] + AVARGIYA

# Keyboard diagram: key -> (normal, shift) output.
KEY_OUT = {
    "a": ("ಅ", "ಆ"), "b": ("ಬ", "ಭ"), "c": ("ಚ", "ಛ"), "d": ("ದ", "ಧ"), "e": ("ಎ", "ಏ"),
    "f": ("್", "್‍"), "g": ("ಗ", "ಘ"), "h": ("ಹ", "ಃ"), "i": ("ಇ", "ಈ"), "j": ("ಜ", "ಝ"),
    "k": ("ಕ", "ಖ"), "l": ("ಲ", "ಳ"), "m": ("ಮ", "ಂ"), "n": ("ನ", "ಣ"), "o": ("ಒ", "ಓ"),
    "p": ("ಪ", "ಫ"), "q": ("ಟ", "ಠ"), "r": ("ರ", "ಋ"), "s": ("ಸ", "ಶ"), "t": ("ತ", "ಥ"),
    "u": ("ಉ", "ಊ"), "v": ("ವ", "ಔ"), "w": ("ಡ", "ಢ"), "x": ("ಷ", "X"), "y": ("ಯ", "ಐ"),
    "z": ("ಞ", "ಙ"),
}
KEY_ROWS = ["qwertyuiop", "asdfghjkl", "zxcvbnm"]

# Every (keys, expected Kannada) pair used on any page; verified by build.py with the JS engine.
TYPED_EXAMPLES: list[tuple[str, str]] = []


def keys_html(keys: str) -> str:
    """'krfnAqk' -> <kbd>k</kbd><kbd>r</kbd>…  (space shown as a Space key)."""
    parts = []
    for ch in keys:
        parts.append('<kbd>Space</kbd>' if ch == " " else f"<kbd>{esc(ch)}</kbd>")
    return '<span class="keys">' + "".join(parts) + "</span>"


def typed(keys: str, kannada: str) -> str:
    TYPED_EXAMPLES.append((keys, kannada))
    return keys_html(keys)


def kn(text: str) -> str:
    return f'<span lang="kn">{esc(text)}</span>'


def example_table(rows, meaning=True, caption=None) -> str:
    """rows: (keys, kannada[, english meaning])"""
    head = "<tr><th>Keys to press</th><th>Result</th>" + ("<th>Meaning</th>" if meaning else "") + "</tr>"
    body = []
    for row in rows:
        k, t = row[0], row[1]
        m = f"<td>{esc(row[2])}</td>" if meaning and len(row) > 2 else ("<td></td>" if meaning else "")
        body.append(f'<tr><td>{typed(k, t)}</td><td lang="kn">{esc(t)}</td>{m}</tr>')
    cap = f"<caption>{esc(caption)}</caption>" if caption else ""
    return (f'<div class="table-scroll"><table class="examples">{cap}<thead>{head}</thead>'
            f'<tbody>{"".join(body)}</tbody></table></div>')


def key_cell(glyph: str, keys: str) -> str:
    return f'<div class="key"><span class="glyph" lang="kn">{esc(glyph)}</span><kbd>{esc(keys)}</kbd></div>'


def letter_chart(caption="Nudi / KGP key → Kannada letter", specials=True) -> str:
    vowels = "".join(key_cell(g, k) for g, k in VOWELS[:13]) + key_cell("ಂ", "M") + key_cell("ಃ", "H")
    vargas = []
    for row in VARGAS:
        cells = [key_cell(*c) if c else '<div class="key key-empty" aria-hidden="true"></div>' for c in row]
        vargas.append('<div class="key-row">' + "".join(cells) + "</div>")
    other = "".join(key_cell(*c) for c in AVARGIYA)
    out = [f'<figure class="chart"><figcaption>{esc(caption)}</figcaption>',
           '<h3>Vowels and signs (<span lang="kn">ಸ್ವರಗಳು</span>)</h3>',
           f'<div class="key-grid">{vowels}</div>',
           '<h3>Consonants by group (<span lang="kn">ವರ್ಗೀಯ ವ್ಯಂಜನಗಳು</span>)</h3>',
           f'<div class="varga">{"".join(vargas)}</div>',
           '<h3>Other consonants (<span lang="kn">ಅವರ್ಗೀಯ ವ್ಯಂಜನಗಳು</span>)</h3>',
           f'<div class="key-grid">{other}</div>']
    if specials:
        sp = "".join(key_cell(*c) for c in SPECIAL) + key_cell("್", "f") + key_cell("್‍", "F")
        out += ['<h3>Halant and special letters</h3>', f'<div class="key-grid">{sp}</div>']
    out.append("</figure>")
    return "\n".join(out)


def keyboard_diagram() -> str:
    rows = []
    for i, row in enumerate(KEY_ROWS):
        keys = []
        for ch in row:
            lo, up = KEY_OUT[ch]
            up_html = (f'<span class="kb-up" lang="kn">{esc(up)}</span>' if up and up != "X"
                       else ('<span class="kb-up kb-mod">X</span>' if up == "X" else '<span class="kb-up"></span>'))
            keys.append(f'<div class="kb-key">{up_html}<span class="kb-lo" lang="kn">{esc(lo)}</span>'
                        f'<span class="kb-en">{ch.upper()}</span></div>')
        rows.append(f'<div class="kb-row kb-row-{i}">' + "".join(keys) + "</div>")
    return ('<figure class="chart kb"><figcaption>Nudi / KGP keyboard layout – '
            'lower letter = key alone, upper letter = <kbd>Shift</kbd> + key</figcaption>'
            '<div class="kb-board" role="img" aria-label="Nudi Kannada keyboard layout on a QWERTY keyboard">'
            + "".join(rows) + "</div>"
            '<p class="kb-note"><kbd>f</kbd> adds the halant (<span lang="kn">್</span>) and joins the next consonant '
            '(ottakshara). <kbd>Shift</kbd>+<kbd>F</kbd> gives a visible halant. <kbd>X</kbd> after '
            '<kbd>r</kbd>, <kbd>L</kbd> or <kbd>R</kbd> gives <span lang="kn">ಱ ೞ ೠ</span>.</p></figure>')


def kagunita_table(consonants=None, caption="Kagunita (ಕಾಗುಣಿತ) with Nudi keys") -> str:
    consonants = consonants or CONSONANTS
    head = "<tr><th></th>" + "".join(
        f'<th><span lang="kn">{esc(("ಅ" + m) if m and m in "ಂಃ" else (VOWELS[i][0] if i < 13 else ""))}</span></th>'
        for i, (m, _) in enumerate(MATRAS)) + "</tr>"
    body = []
    for g, k in consonants:
        cells = [f'<th lang="kn">{esc(g)}</th>']
        for m, mk in MATRAS:
            keys = k + ("" if mk == "a" else mk)
            glyph = g + m
            TYPED_EXAMPLES.append((keys, glyph))
            cells.append(f'<td><span class="glyph" lang="kn">{esc(glyph)}</span><kbd>{esc(keys)}</kbd></td>')
        body.append("<tr>" + "".join(cells) + "</tr>")
    return (f'<div class="table-scroll"><table class="kagunita"><caption>{caption}</caption>'
            f"<thead>{head}</thead><tbody>{''.join(body)}</tbody></table></div>")


def try_box(placeholder="Type here using Nudi keys – e.g. k n f n w gives ಕನ್ನಡ",
            title="Try it: type Kannada with Nudi keys") -> str:
    return f"""<section class="try-box">
            <div class="try-head"><h2>{esc(title)}</h2>
                <button type="button" class="mode" data-nudi-toggle aria-pressed="true" title="Switch Kannada / English (F9)">KAN</button></div>
            <textarea data-nudi-type lang="kn" rows="3" spellcheck="false" autocapitalize="off" autocomplete="off" aria-label="Kannada typing practice box" placeholder="{esc(placeholder)}"></textarea>
            <p class="try-note">Practice box – press <kbd>F9</kbd> to switch between Kannada and English. For saving, formatting and Word export, use the <a href="/">full editor</a>.</p>
        </section>"""


def related(slugs_titles) -> str:
    items = "".join(f'<li><a href="{u}">{esc(t)}</a></li>' for u, t in slugs_titles)
    return f'<section class="no-print"><h2>Related guides</h2><ul class="related">{items}</ul></section>'


def print_button(label="Download as PDF / Print") -> str:
    return (f'<p class="no-print"><button type="button" class="btn btn-outline" onclick="window.print()">'
            f'{esc(label)}</button> <span class="hint">Choose “Save as PDF” in the print dialog.</span></p>')


# ---------------------------------------------------------------------------
# Page shell
# ---------------------------------------------------------------------------
def nav_html(current: str) -> str:
    return "\n".join(
        f'<a href="{u}"' + (' aria-current="page"' if u == current else "") + f">{esc(t)}</a>" for u, t in NAV)


def footer_items_html() -> str:
    return "\n".join(f'<li><a href="{u}">{esc(t)}</a></li>' for u, t in FOOTER)


def faq_html(faqs) -> str:
    items = "\n".join(f"<details><summary>{esc(q)}</summary><p>{a}</p></details>" for q, a in faqs)
    return f'<section class="faq"><h2>Frequently asked questions</h2>\n{items}\n</section>'


def strip_tags(s: str) -> str:
    import re
    return html.unescape(re.sub(r"<[^>]+>", "", s))


def render(page: dict) -> str:
    slug = page["slug"]
    url = f"{SITE}/{slug}/"
    path = f"/{slug}/"
    title, desc = page["title"], page["description"]
    graph = [
        {"@type": "WebPage", "@id": url, "url": url, "name": title, "description": desc,
         "inLanguage": "en", "isPartOf": {"@id": f"{SITE}/#website"}, "about": {"@id": f"{SITE}/#app"}},
        {"@type": "BreadcrumbList", "itemListElement": [
            {"@type": "ListItem", "position": 1, "name": SITE_NAME, "item": f"{SITE}/"},
            {"@type": "ListItem", "position": 2, "name": page["crumb"], "item": url}]},
        {"@type": "WebSite", "@id": f"{SITE}/#website", "url": f"{SITE}/", "name": SITE_NAME},
    ]
    if page.get("app"):
        graph.append({"@type": "WebApplication", "name": page["app"], "url": url,
                      "applicationCategory": "UtilitiesApplication", "operatingSystem": "Any (web browser)",
                      "offers": {"@type": "Offer", "price": "0", "priceCurrency": "INR"},
                      "inLanguage": ["en", "kn"]})
    if page.get("howto"):
        name, steps = page["howto"]
        graph.append({"@type": "HowTo", "name": name, "step": [
            {"@type": "HowToStep", "position": i + 1, "text": strip_tags(s)} for i, s in enumerate(steps)]})
    if page.get("faqs"):
        graph.append({"@type": "FAQPage", "mainEntity": [
            {"@type": "Question", "name": q, "acceptedAnswer": {"@type": "Answer", "text": strip_tags(a)}}
            for q, a in page["faqs"]]})
    ld = json.dumps({"@context": "https://schema.org", "@graph": graph}, ensure_ascii=False, indent=2)

    scripts = "".join(f'\n    <script src="{s}" defer></script>' for s in page.get("scripts", []))
    hero_cta = page.get("hero_cta", '<p><a class="btn" href="/">Start typing in Kannada – it\'s free</a></p>')
    faq = faq_html(page["faqs"]) if page.get("faqs") else ""

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>{esc(title, quote=False)}</title>
    <meta name="description" content="{esc(desc)}" />
    <meta name="robots" content="index, follow, max-image-preview:large" />
    <meta name="theme-color" content="#1b6ec2" />
    <link rel="canonical" href="{url}" />
    <meta property="og:type" content="website" />
    <meta property="og:site_name" content="{SITE_NAME}" />
    <meta property="og:url" content="{url}" />
    <meta property="og:title" content="{esc(title)}" />
    <meta property="og:description" content="{esc(desc)}" />
    <meta property="og:image" content="{SITE}/nudi_logo.png" />
    <meta property="og:locale" content="en_IN" />
    <meta name="twitter:card" content="summary" />
    <link rel="icon" type="image/png" href="/Seal_of_Karnataka.png" />
    <link rel="apple-touch-icon" href="/nudi_logo.png" />
    <link rel="manifest" href="/manifest.webmanifest" />
    <link rel="stylesheet" href="/css/fonts.css" />
    <link rel="stylesheet" href="/css/landing.css" />{scripts}
    <script type="application/ld+json">
{ld}
    </script>
</head>
<body>
    <header class="site-header">
        <div class="wrap header-inner">
            <a class="brand" href="/"><img src="/nudi_logo.png" alt="" width="36" height="36" /><span>Kannada Nudi</span></a>
            <nav class="site-nav" aria-label="Main">
{nav_html(path)}
            </nav>
            <a class="btn btn-small" href="/">Open Editor</a>
        </div>
    </header>

    <main class="wrap">
        <nav class="crumbs" aria-label="Breadcrumb"><a href="/">{SITE_NAME}</a> › <span>{esc(page["crumb"])}</span></nav>
        <section class="hero">
            <h1>{esc(page["h1"])}</h1>
            <p class="kn-tagline" lang="kn">{esc(page["tagline"])}</p>
            <p class="lead">{page["lead"]}</p>
            {hero_cta}
        </section>
{page["body"]}
{faq}
        <section class="cta">
            <h2>{esc(page.get("cta_title", "Ready to write in Kannada?"))}</h2>
            <p>{page.get("cta_text", "The editor runs entirely in your browser – no sign-up, no installation.")}</p>
            <a class="btn" href="/">Open Kannada Nudi Editor</a>
        </section>
    </main>

    <footer class="site-footer">
        <div class="wrap footer-inner">
            <div>
                <a class="brand" href="/"><img src="/nudi_logo.png" alt="" width="28" height="28" /><span>Kannada Nudi</span></a>
                <p>Free Kannada editor. Developed by <a href="https://codegres.com/">Codegres</a>, guided by <a href="https://kagapa.com/">KAGAPA</a>.</p>
                <p class="stores"><a href="{STORE_WINDOWS}" rel="noopener">Get it on Microsoft Store</a> · <a href="{STORE_PLAY}" rel="noopener">Get it on Google Play</a> · <a href="{STORE_APPLE}" rel="noopener">Download on the App Store</a></p>
            </div>
            <ul>
{footer_items_html()}
            </ul>
        </div>
    </footer>
</body>
</html>
"""
