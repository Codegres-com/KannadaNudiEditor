"""Software pages: Nudi app for PC, Windows 11, MS Word."""
from common import (example_table, related, STORE_WINDOWS, STORE_APPLE, STORE_SNAP, STORE_PLAY)

STORE_BTNS = (f'<p><a class="btn" href="{STORE_WINDOWS}" rel="noopener">Get it from Microsoft Store</a> '
              f'<a class="btn btn-outline" href="/">Use it online</a></p>')

app_pc = {
    "slug": "kannada-nudi-app-for-pc",
    "title": "Kannada Nudi Typing App for PC – Free Download for Windows, Mac & Linux",
    "description": "Download the free Kannada Nudi typing app for PC. Nudi and Baraha keyboards, Word (.docx) export, Nudi-to-Unicode conversion and F9 Global Mode to type Kannada in any app.",
    "crumb": "Kannada Nudi App for PC",
    "h1": "Kannada Nudi Typing App for PC",
    "tagline": "ಕಂಪ್ಯೂಟರ್‌ಗಾಗಿ ಉಚಿತ ಕನ್ನಡ ನುಡಿ ಟೈಪಿಂಗ್ ತಂತ್ರಾಂಶ",
    "lead": "Kannada Nudi Editor is a free Kannada typing app for desktop computers. It uses the standard <strong>Nudi / KGP</strong> keyboard (plus phonetic Baraha), writes Unicode Kannada, opens and saves Word files and works fully offline.",
    "hero_cta": STORE_BTNS,
    "body": f"""
        <section>
            <h2>Download for your computer</h2>
            <div class="platforms">
                <div class="platform"><h3>Windows 10 / 11</h3><p>From the Microsoft Store – installs in one click and updates automatically.</p><a class="btn" href="{STORE_WINDOWS}" rel="noopener">Microsoft Store</a></div>
                <div class="platform"><h3>macOS</h3><p>Apple Silicon and Intel Macs.</p><a class="btn" href="{STORE_APPLE}" rel="noopener">Mac App Store</a></div>
                <div class="platform"><h3>Linux</h3><p>Ubuntu and any Snap-enabled distro: <code>sudo snap install kannadanudi</code></p><a class="btn" href="{STORE_SNAP}" rel="noopener">Snap Store</a></div>
            </div>
            <p>Can't install software on your PC (office or college computer)? The <a href="/">online editor</a> has the same features in the browser. For phones see <a href="/download/">all downloads</a>.</p>
        </section>

        <section>
            <h2>What you get</h2>
            <ul class="features">
                <li><strong>Nudi / KGP keyboard</strong> – the same layout as KAGAPA Nudi 6.1, so there is nothing to relearn. <a href="/kannada-nudi-typing-keyboard-chart/">See the chart</a>.</li>
                <li><strong>Baraha phonetic typing</strong> – type <code>kannaDa</code> to get <span lang="kn">ಕನ್ನಡ</span>.</li>
                <li><strong>Global Mode (<kbd>F9</kbd>)</strong> on Windows and Linux – type Kannada directly in MS Word, Excel, PowerPoint, Chrome, WhatsApp Desktop and other apps.</li>
                <li><strong>Word-style editor</strong> – fonts, headings, tables and formulas, with Word (.docx) export.</li>
                <li><strong>Old Nudi files</strong> – open Nudi 4.0 ASCII documents and convert them to Unicode. <a href="/nudi-to-unicode/">How it works</a>.</li>
                <li><strong>Kannada speech to text</strong> – dictate instead of typing in the <a href="/kannada-speech-to-text/">online editor</a> and mobile apps.</li>
                <li><strong>Offline and private</strong> – nothing is uploaded; no account needed.</li>
            </ul>
        </section>

        <section>
            <h2>Install and start typing on Windows</h2>
            <ol class="steps">
                <li>Open the <a href="{STORE_WINDOWS}">Kannada Nudi Editor Store page</a> and click <strong>Get</strong> / <strong>Install</strong>.</li>
                <li>Launch <strong>Kannada Nudi</strong> from the Start menu.</li>
                <li>Type in the editor using Nudi keys – e.g. <kbd>k</kbd><kbd>n</kbd><kbd>f</kbd><kbd>n</kbd><kbd>w</kbd> → <span lang="kn">ಕನ್ನಡ</span>.</li>
                <li>To type in another program, leave the app running, switch to that program and press <kbd>F9</kbd>. Press <kbd>F9</kbd> again for English.</li>
            </ol>
        </section>
{related([("/kannada-typing-software-for-windows-11/", "Kannada typing on Windows 11"), ("/nudi-kannada-typing-in-ms-word/", "Nudi typing in MS Word"), ("/how-to-type-in-nudi-kannada/", "How to type in Nudi")])}""",
    "faqs": [
        ("Is the Kannada Nudi app for PC free?", "Yes. All versions – Windows, Mac, Linux, Android, iPhone and the web editor – are free."),
        ("Is this the official Nudi software from KAGAPA?", "Kannada Nudi Editor is developed by Codegres with guidance from KAGAPA and uses the same Nudi / KGP keyboard layout. KAGAPA's own Nudi software is distributed separately on kagapa.com."),
        ("Does it work on Windows 7 or 8?", "The Microsoft Store app needs Windows 10 or 11. On older Windows versions use the <a href=\"/\">online editor</a> in a current browser."),
        ("Can I type Kannada in MS Word with it?", "Yes. Press <kbd>F9</kbd> to turn on Global Mode and type in Word directly, or write in the app and save as .docx. See <a href=\"/nudi-kannada-typing-in-ms-word/\">Nudi typing in MS Word</a>."),
        ("Does it need internet?", "No. The desktop apps work fully offline."),
    ],
}

# ---------------------------------------------------------------------------
win11 = {
    "slug": "kannada-typing-software-for-windows-11",
    "title": "Kannada Typing Software for Windows 11 – Free Nudi & Baraha Keyboard",
    "description": "Free Kannada typing software for Windows 11: install from the Microsoft Store, press F9 to type Kannada with Nudi or Baraha in Word, Excel, Chrome and any app. Unicode output.",
    "crumb": "Kannada Typing Software for Windows 11",
    "h1": "Kannada Typing Software for Windows 11",
    "tagline": "ವಿಂಡೋಸ್ 11ಗೆ ಉಚಿತ ಕನ್ನಡ ಟೈಪಿಂಗ್ ತಂತ್ರಾಂಶ",
    "lead": "Kannada Nudi Editor is a Microsoft Store app built for Windows 11 and 10. It gives you the Nudi and Baraha keyboards inside every program through <strong>Global Mode</strong> – press <kbd>F9</kbd> and type Kannada in Word, Excel, Outlook, Chrome or Edge.",
    "hero_cta": STORE_BTNS,
    "howto": ("Set up Kannada typing on Windows 11", [
        "Open the Microsoft Store page for Kannada Nudi Editor and click Install.",
        "Start Kannada Nudi from the Start menu.",
        "Switch to the app where you want to type, such as Word or Chrome.",
        "Press F9 to turn on Kannada and type with Nudi keys, for example k n f n w for ಕನ್ನಡ.",
        "Press F9 again to go back to English.",
    ]),
    "body": f"""
        <section>
            <h2>Set up Kannada typing on Windows 11</h2>
            <ol class="steps">
                <li>Open the <a href="{STORE_WINDOWS}">Microsoft Store page</a> and click <strong>Install</strong>. No administrator rights or separate fonts needed.</li>
                <li>Start <strong>Kannada Nudi</strong> from the Start menu (pin it to the taskbar for quick access).</li>
                <li>Click into the app where you want Kannada – Word, Excel, Chrome, Notepad.</li>
                <li>Press <kbd>F9</kbd> and type using Nudi keys: <kbd>k</kbd><kbd>n</kbd><kbd>f</kbd><kbd>n</kbd><kbd>w</kbd> → <span lang="kn">ಕನ್ನಡ</span>.</li>
                <li>Press <kbd>F9</kbd> again to return to English.</li>
            </ol>
            <p>Choose Nudi or Baraha in the app's Layout menu; Global Mode uses the same layout.</p>
        </section>

        <section>
            <h2>Why use it instead of the built-in Windows Kannada keyboard?</h2>
            <p>Windows 11 can add a Kannada keyboard under <em>Settings › Time &amp; language › Language &amp; region</em>, but it uses a different (InScript-style) arrangement. If you learned typing on Nudi – the layout used in Karnataka offices and typing exams – Kannada Nudi Editor keeps the keys you already know.</p>
            <div class="table-scroll"><table class="examples"><thead><tr><th></th><th>Kannada Nudi Editor</th><th>Windows built-in keyboard</th></tr></thead><tbody>
                <tr><td>Nudi / KGP layout</td><td>Yes</td><td>No</td></tr>
                <tr><td>Baraha phonetic typing</td><td>Yes</td><td>No</td></tr>
                <tr><td>One key to switch (<kbd>F9</kbd>)</td><td>Yes</td><td><kbd>Win</kbd>+<kbd>Space</kbd></td></tr>
                <tr><td>Convert old Nudi ASCII files</td><td>Yes</td><td>No</td></tr>
                <tr><td>Unicode output</td><td>Yes</td><td>Yes</td></tr>
            </tbody></table></div>
        </section>

        <section>
            <h2>Quick Nudi examples</h2>
            {example_table([("knfnw", "ಕನ್ನಡ", "Kannada"), ("krfnAqk", "ಕರ್ನಾಟಕ", "Karnataka"), ("nmsfkAr", "ನಮಸ್ಕಾರ", "Hello")])}
            <p>All keys are on the <a href="/kannada-nudi-typing-keyboard-chart/">Nudi keyboard chart</a>.</p>
        </section>
{related([("/kannada-nudi-app-for-pc/", "Kannada Nudi app for PC"), ("/nudi-kannada-typing-in-ms-word/", "Nudi typing in MS Word"), ("/type-kannada-using-english-keyboard/", "Type Kannada with an English keyboard")])}""",
    "faqs": [
        ("What is the best Kannada typing software for Windows 11?", "For Nudi or Baraha typists, Kannada Nudi Editor from the <a href=\"" + STORE_WINDOWS + "\">Microsoft Store</a> is free, uses the standard layouts and types into any app with <kbd>F9</kbd>."),
        ("Does Global Mode work in apps run as Administrator?", "No. Windows blocks keyboard helpers from elevated apps. Run the target app normally, or use the editor and copy the text."),
        ("Does Nudi 6.1 work on Windows 11?", "If you prefer KAGAPA's Nudi 6.1, check kagapa.com for compatibility. Kannada Nudi Editor offers the same layout and is built for Windows 11."),
        ("Which font should I use for Kannada in Word on Windows 11?", "Any Unicode Kannada font: Nirmala UI and Tunga ship with Windows; NudiParijatha and Noto Sans Kannada are free downloads."),
    ],
}

# ---------------------------------------------------------------------------
msword = {
    "slug": "nudi-kannada-typing-in-ms-word",
    "title": "Nudi Kannada Typing in MS Word – Nudi 5.0, 6.0 & 6.1 Guide",
    "description": "How to type Kannada in MS Word with Nudi 5.0, Nudi 6.1 or Kannada Nudi Editor. Fix boxes and English gibberish, pick the right font, and convert old Nudi documents to Unicode.",
    "crumb": "Nudi Kannada Typing in MS Word",
    "h1": "Nudi Kannada Typing in MS Word",
    "tagline": "ಎಂಎಸ್ ವರ್ಡ್‌ನಲ್ಲಿ ನುಡಿ ಕನ್ನಡ ಟೈಪಿಂಗ್",
    "lead": "Want to type Kannada in Microsoft Word with the Nudi keyboard? You can use KAGAPA's Nudi software (5.0, 6.0, 6.1) or Kannada Nudi Editor's <kbd>F9</kbd> Global Mode. The key to avoiding gibberish is matching <strong>Unicode vs ASCII</strong> mode with the right font.",
    "howto": ("Type Nudi Kannada in MS Word", [
        "Install Kannada Nudi Editor from the Microsoft Store and start it.",
        "Open Microsoft Word and click in the document.",
        "Press F9 to switch to Kannada.",
        "Type with Nudi keys, for example k n f n w for ಕನ್ನಡ.",
        "Press F9 to go back to English. Use a Unicode Kannada font such as Nirmala UI or Tunga.",
    ]),
    "body": f"""
        <section>
            <h2>Method 1: Kannada Nudi Editor Global Mode (Unicode)</h2>
            <ol class="steps">
                <li>Install <a href="{STORE_WINDOWS}">Kannada Nudi Editor from the Microsoft Store</a> and start it.</li>
                <li>Open Word and click in your document.</li>
                <li>Press <kbd>F9</kbd> to switch to Kannada and type with Nudi keys – <kbd>k</kbd><kbd>n</kbd><kbd>f</kbd><kbd>n</kbd><kbd>w</kbd> → <span lang="kn">ಕನ್ನಡ</span>.</li>
                <li>Press <kbd>F9</kbd> again to type English in the same document.</li>
                <li>Keep a Unicode Kannada font: <strong>Nirmala UI</strong>, <strong>Tunga</strong>, <strong>NudiParijatha</strong> or <strong>Noto Sans Kannada</strong>.</li>
            </ol>
            <p>Not on Windows? Write in the <a href="/">online editor</a> and click <strong>Save</strong> to download a .docx that opens in Word.</p>
        </section>

        <section>
            <h2>Method 2: KAGAPA Nudi 5.0 / 6.0 / 6.1 in Word</h2>
            <ol class="steps">
                <li>Start the Nudi software; it runs in the Windows taskbar.</li>
                <li>Choose <strong>Unicode</strong> mode if your version offers it (Nudi 5.0 and later). Nudi 4.0 and older only have ASCII mode.</li>
                <li>In Word, choose the matching font: a Unicode Kannada font in Unicode mode, or a Nudi font such as <em>Nudi 01 e</em> in ASCII mode.</li>
                <li>Switch Nudi to Kannada from its taskbar icon and type with the same Nudi keys.</li>
            </ol>
        </section>

        <section>
            <h2>Unicode vs ASCII – why Kannada turns into gibberish</h2>
            <div class="table-scroll"><table class="examples"><thead><tr><th></th><th>Unicode (recommended)</th><th>ASCII / legacy Nudi</th></tr></thead><tbody>
                <tr><td>Used by</td><td>Nudi 5.0+ in Unicode mode, Nudi 6.x, Kannada Nudi Editor</td><td>Nudi 4.0 and older; Nudi 5.0+ in ASCII mode</td></tr>
                <tr><td>Font in Word</td><td>Nirmala UI, Tunga, NudiParijatha…</td><td>Nudi 01 e, Nudi 01 k, Nudi Akshara…</td></tr>
                <tr><td>On another computer</td><td>Displays correctly</td><td>Shows <code>PÀ£ÀßqÀ</code> unless the same font is installed</td></tr>
                <tr><td>Phones, email, web, search</td><td>Works</td><td>Breaks</td></tr>
            </tbody></table></div>
            <p>If an old document shows English-looking characters such as <code>PÀ£ÀßqÀ</code>, it is ASCII Nudi. Convert it with the <a href="/nudi-ascii-to-unicode/">Nudi ASCII to Unicode converter</a> (paste text) or <a href="/nudi-to-unicode/">open the .docx in the editor</a> to convert the whole file with formatting.</p>
        </section>

        <section>
            <h2>Common keys to remember in Word</h2>
            {example_table([("knfnw", "ಕನ್ನಡ", "Kannada"), ("krfnAqk srfkAr", "ಕರ್ನಾಟಕ ಸರ್ಕಾರ", "Government of Karnataka"), ("ptfr", "ಪತ್ರ", "Letter"), ("vixy", "ವಿಷಯ", "Subject")])}
        </section>
{related([("/kannada-typing-software-for-windows-11/", "Kannada typing software for Windows 11"), ("/nudi-6-to-nudi-4-converter/", "Nudi 6 (Unicode) to Nudi 4 (ASCII) converter"), ("/kannada-nudi-typing-keyboard-chart/", "Nudi keyboard chart (PDF)")])}""",
    "faqs": [
        ("How do I type Kannada in MS Word using Nudi 6.1?", "Start Nudi 6.1, keep it in Unicode mode, open Word, select a Unicode Kannada font such as Nirmala UI and switch Nudi to Kannada. Alternatively install <a href=\"" + STORE_WINDOWS + "\">Kannada Nudi Editor</a> and press <kbd>F9</kbd> in Word."),
        ("How do I type in Word with Nudi 5.0?", "Nudi 5.0 can type in ASCII or Unicode. Choose Unicode mode with a Unicode font for documents you will share; in ASCII mode you must use a Nudi font like Nudi 01 e."),
        ("Why do I see boxes instead of Kannada in Word?", "The selected font does not contain Kannada. Select the text and change the font to Nirmala UI or Tunga."),
        ("Why does my Nudi document look like English letters on another computer?", "It was typed in ASCII mode with a Nudi font that the other computer doesn't have. Convert it to Unicode with the <a href=\"/nudi-ascii-to-unicode/\">Nudi ASCII to Unicode converter</a>."),
        ("Can I type Kannada in Word on a Mac?", "Yes – write in <a href=\"" + STORE_APPLE + "\">Kannada Nudi Editor for Mac</a> and save as .docx, or copy the Unicode text into Word."),
    ],
}

PAGES = [app_pc, win11, msword]
