"""Converter tool pages: Nudi ASCII -> Unicode, and Unicode (Nudi 6) -> ASCII (Nudi 4)."""
from common import related, esc

TOOL_SCRIPTS = ["/js/nudiConverter.js", "/js/converterTool.js"]

A2U_SAMPLE = "__A2U_SAMPLE__"  # filled in by build.py from U2A_SAMPLE via the real converter
U2A_SAMPLE = "ಕನ್ನಡ ನುಡಿ ಸಂಪಾದಕ. ಕರ್ನಾಟಕ ಸರ್ಕಾರ. ಶ್ರೀ ಸತ್ಯವಾದ. ಜ್ಞಾನ ಮತ್ತು ವಿಜ್ಞಾನ."


def tool(direction: str, sample: str) -> str:
    a2u = direction == "a2u"
    in_label = "Old Nudi (ASCII) text" if a2u else "Unicode Kannada text (Nudi 6 / Nudi 5 Unicode)"
    out_label = "Unicode Kannada" if a2u else "Nudi ASCII text (Nudi 4 format)"
    in_ph = ("Paste old Nudi text here, e.g. PÀ£ÀßqÀ" if a2u else "Paste or type Unicode Kannada here, e.g. ಕನ್ನಡ")
    font_label = ("Show input in Nudi font" if a2u else "Show output in Nudi font")
    in_lang = "" if a2u else ' lang="kn"'
    out_lang = ' lang="kn"' if a2u else ""
    mixed = ("""<label class="opt"><input type="checkbox" data-mixed /> Keep English words unchanged (mixed English + Nudi text)</label>"""
             if a2u else "")
    return f"""<section class="tool" data-direction="{direction}" data-sample="{esc(sample)}">
            <div class="tool-grid">
                <div class="tool-pane">
                    <label for="tool-in-{direction}">{in_label}</label>
                    <textarea id="tool-in-{direction}" data-in{in_lang} rows="10" spellcheck="false" autocomplete="off" placeholder="{esc(in_ph)}"></textarea>
                    <div class="tool-actions">
                        <button type="button" class="btn btn-small btn-outline" data-action="sample">Sample text</button>
                        <label class="btn btn-small btn-outline file-btn">Open .txt file<input type="file" accept=".txt,text/plain" data-file hidden /></label>
                        <button type="button" class="btn btn-small btn-outline" data-action="clear">Clear</button>
                    </div>
                </div>
                <div class="tool-pane">
                    <label for="tool-out-{direction}">{out_label}</label>
                    <textarea id="tool-out-{direction}" data-out{out_lang} rows="10" readonly placeholder="Converted text appears here as you type"></textarea>
                    <div class="tool-actions">
                        <button type="button" class="btn btn-small" data-action="copy">Copy</button>
                        <button type="button" class="btn btn-small btn-outline" data-action="download">Download .txt</button>
                    </div>
                </div>
            </div>
            <div class="tool-options">
                <label class="opt"><input type="checkbox" data-font-toggle checked /> {font_label}</label>
                {mixed}
            </div>
            <p class="tool-status" data-status role="status" aria-live="polite"></p>
            <noscript><p>The converter needs JavaScript.</p></noscript>
        </section>"""


a2u = {
    "slug": "nudi-ascii-to-unicode",
    "title": "Nudi ASCII to Unicode Converter – Convert Old Nudi Format to Kannada",
    "description": "Convert old Nudi format (ASCII) text to readable Unicode Kannada instantly. Paste text typed in Nudi 4.0 / Nudi 01 e fonts and get Kannada that works everywhere. Free, private, in-browser.",
    "crumb": "Nudi ASCII to Unicode Converter",
    "h1": "Nudi ASCII to Unicode Converter",
    "tagline": "ಹಳೆಯ ನುಡಿ (ASCII) ಪಠ್ಯವನ್ನು ಯೂನಿಕೋಡ್ ಕನ್ನಡಕ್ಕೆ ಪರಿವರ್ತಿಸಿ",
    "lead": "Convert Nudi format to Kannada: paste text from an old Nudi document – the kind that looks like <code>PÀ£ÀßqÀ £ÀÄr</code> – and get proper Unicode <span lang=\"kn\">ಕನ್ನಡ ನುಡಿ</span> you can paste into Word, email, WhatsApp or a website.",
    "app": "Nudi ASCII to Unicode Converter",
    "hero_cta": "",
    "scripts": TOOL_SCRIPTS,
    "body": f"""
{tool("a2u", A2U_SAMPLE)}
        <section>
            <h2>How to convert old Nudi format to Kannada</h2>
            <ol class="steps">
                <li>Open your old document in Word (or any program), select the Kannada text and copy it (<kbd>Ctrl</kbd>+<kbd>C</kbd>).</li>
                <li>Paste it into the left box above. The converted Unicode Kannada appears on the right instantly.</li>
                <li>Click <strong>Copy</strong> and paste the result wherever you need it, or <strong>Download .txt</strong>.</li>
                <li>If the text mixes English and Kannada, tick <strong>Keep English words unchanged</strong>.</li>
            </ol>
        </section>

        <section>
            <h2>Convert a whole Word file with formatting</h2>
            <p>For .docx files, the <a href="/">Kannada Nudi Editor</a> converts the entire document while keeping headings, bold, tables and layout – and only converts text in Nudi fonts, so English paragraphs stay English. Open the file and choose <strong>ASCII (Nudi/Baraha)</strong> on import. <a href="/nudi-to-unicode/">Step-by-step guide →</a></p>
        </section>

        <section>
            <h2>What is “old Nudi format”?</h2>
            <p>Before Unicode, Kannada software such as Nudi 4.0 and Baraha stored each Kannada glyph as a Latin-1 character (À, Á, ª, £ …) and relied on a special font – <em>Nudi 01 e</em>, <em>Nudi 01 k</em>, <em>Nudi Akshara</em> – to draw them as Kannada. That is called ASCII or ANSI Kannada. Without the exact font, the text shows as gibberish, cannot be searched or spell-checked, and breaks on phones and websites.</p>
            <p>Unicode gives every Kannada letter its own standard code, so it displays correctly with any Kannada font on any device. Converting Nudi ASCII to Unicode is a one-time fix for old documents.</p>
            <div class="table-scroll"><table class="examples"><thead><tr><th>Old Nudi (ASCII)</th><th>Unicode Kannada</th></tr></thead><tbody>
                <tr><td><code>PÀ£ÀßqÀ</code></td><td lang="kn">ಕನ್ನಡ</td></tr>
                <tr><td><code>PÀ£ÁðlPÀ</code></td><td lang="kn">ಕರ್ನಾಟಕ</td></tr>
                <tr><td><code>£ÀªÀÄ¸ÁÌgÀ</code></td><td lang="kn">ನಮಸ್ಕಾರ</td></tr>
                <tr><td><code>eÁÕ£À</code></td><td lang="kn">ಜ್ಞಾನ</td></tr>
            </tbody></table></div>
        </section>

        <section>
            <h2>Private by design</h2>
            <p>The conversion runs entirely in your browser. Your text is never uploaded to a server, so it is safe for office, legal and government documents.</p>
        </section>
{related([("/nudi-6-to-nudi-4-converter/", "Reverse: Unicode to Nudi ASCII (Nudi 6 to Nudi 4)"), ("/nudi-to-unicode/", "Convert Nudi Word (.docx) files"), ("/nudi-kannada-typing-in-ms-word/", "Nudi Kannada typing in MS Word")])}""",
    "faqs": [
        ("How do I convert Nudi format to Kannada Unicode?", "Paste the old Nudi text into the left box on this page. The Unicode Kannada appears on the right; click Copy to use it anywhere."),
        ("Which fonts does this converter support?", "Text typed with the Nudi ASCII fonts (Nudi 01 e, Nudi 01 k, Nudi 02 e, Nudi 05 e, Nudi 10 e, Nudi Akshara and similar). Baraha ANSI text uses a closely related encoding and usually converts as well."),
        ("Does it handle ottaksharas, arkavattu and vowel signs?", "Yes. Conjuncts, ra-ottu, arkavattu, deergha and split vowel signs are rearranged into correct Unicode order."),
        ("My pasted text already shows as Kannada letters. Do I need to convert it?", "If it shows correctly in any font (for example in a browser or on your phone), it is already Unicode. If it only looks right with a Nudi font selected, it is ASCII and needs converting."),
        ("Is there a file size limit?", "No. Very large texts take a moment longer; for whole Word files use the <a href=\"/nudi-to-unicode/\">editor's file conversion</a>."),
        ("Is the converter free?", "Yes, completely free with no sign-up."),
    ],
}

u2a = {
    "slug": "nudi-6-to-nudi-4-converter",
    "title": "Nudi 6.0 to Nudi 4.0 Converter – Unicode Kannada to Nudi ASCII Online",
    "description": "Convert Nudi 6.0 / 6.1 Unicode Kannada to Nudi 4.0 ASCII format online. Paste Unicode text, copy the Nudi font text into Word with Nudi 01 e. Free and in-browser.",
    "crumb": "Nudi 6 to Nudi 4 Converter",
    "h1": "Nudi 6.0 to Nudi 4.0 Converter",
    "tagline": "ಯೂನಿಕೋಡ್ ಕನ್ನಡವನ್ನು ನುಡಿ 4.0 (ASCII) ರೂಪಕ್ಕೆ ಪರಿವರ್ತಿಸಿ",
    "lead": "Need Kannada in the old Nudi 4.0 format – for DTP software, a printing press or an office template that only works with Nudi fonts? Paste Unicode Kannada typed in Nudi 6.0 / 6.1 (or anywhere else) and get Nudi ASCII text to use with the <em>Nudi 01 e</em> family of fonts.",
    "app": "Nudi 6 to Nudi 4 Converter",
    "hero_cta": "",
    "scripts": TOOL_SCRIPTS,
    "body": f"""
{tool("u2a", U2A_SAMPLE)}
        <section>
            <h2>How to use the converted Nudi 4.0 text</h2>
            <ol class="steps">
                <li>Paste or type Unicode Kannada in the left box (from Nudi 6.x, this editor, a website or WhatsApp).</li>
                <li>Click <strong>Copy</strong> under the right box.</li>
                <li>Paste into Word, PageMaker, CorelDRAW or Photoshop.</li>
                <li>Select the pasted text and set the font to <strong>Nudi 01 e</strong> (or another Nudi ASCII font). It will display as Kannada.</li>
            </ol>
            <p>Without a Nudi font the output looks like <code>PÀ£ÀßqÀ</code> – that is expected; it is how Nudi 4.0 stores Kannada.</p>
        </section>

        <section>
            <h2>Nudi 6.0 vs Nudi 4.0</h2>
            <div class="table-scroll"><table class="examples"><thead><tr><th></th><th>Nudi 6.0 / 6.1</th><th>Nudi 4.0</th></tr></thead><tbody>
                <tr><td>Encoding</td><td>Unicode</td><td>ASCII (font-based)</td></tr>
                <tr><td>Fonts</td><td>Any Unicode Kannada font</td><td>Nudi 01 e, Nudi 01 k, Nudi Akshara…</td></tr>
                <tr><td>Works on phones / web</td><td>Yes</td><td>No</td></tr>
                <tr><td>Old DTP software</td><td>Often not supported</td><td>Supported</td></tr>
            </tbody></table></div>
            <p>Use ASCII only when a tool truly requires it. For everything else, keep Unicode – and use the <a href="/nudi-ascii-to-unicode/">Nudi ASCII to Unicode converter</a> to bring old text back.</p>
        </section>
{related([("/nudi-ascii-to-unicode/", "Reverse: Nudi ASCII to Unicode"), ("/nudi-kannada-typing-in-ms-word/", "Nudi Kannada typing in MS Word"), ("/how-to-type-in-nudi-kannada/", "How to type in Nudi Kannada")])}""",
    "faqs": [
        ("How do I convert Nudi 6.0 text to Nudi 4.0?", "Paste the Unicode Kannada into the left box on this page, copy the result on the right, paste it into your document and apply a Nudi ASCII font such as Nudi 01 e."),
        ("Why does the output look like English letters?", "Nudi 4.0 stores Kannada as Latin characters and needs a Nudi font to display them. Apply Nudi 01 e (or similar) to the pasted text."),
        ("Can I convert back to Unicode later?", "Yes. Use the <a href=\"/nudi-ascii-to-unicode/\">Nudi ASCII to Unicode converter</a>."),
        ("Does it convert Kannada numerals?", "Yes. Kannada digits (೧೨೩) become 1 2 3, which Nudi fonts draw as Kannada numerals."),
        ("Is my text uploaded?", "No. The conversion runs in your browser."),
    ],
}

PAGES = [a2u, u2a]
