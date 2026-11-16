"""Typing guide pages (how to type in Nudi, Karnataka, ksha, learn, English keyboard)."""
from common import (example_table, keys_html, kn, letter_chart, related, try_box, typed,
                    STORE_WINDOWS, STORE_PLAY, STORE_SNAP, STORE_APPLE)

TYPE_BOX_SCRIPTS = ["/js/nudiTypeBox.js"]

# ---------------------------------------------------------------------------
HOW_TO_TYPE_STEPS = [
    'Open the <a href="/">Kannada Nudi Editor</a> in any browser – nothing to install.',
    'Make sure the Input Mode shows <strong>KAN</strong> and the Layout menu is set to <strong>Nudi/KGP</strong> (both are the default).',
    'Type a consonant key to get the full letter with the inherent “a”: <kbd>k</kbd> gives <span lang="kn">ಕ</span>.',
    'Type a vowel key straight after a consonant to add the vowel sign: <kbd>k</kbd><kbd>A</kbd> gives <span lang="kn">ಕಾ</span>.',
    'Press <kbd>f</kbd> between two consonants to join them into an ottakshara: <kbd>k</kbd><kbd>f</kbd><kbd>k</kbd> gives <span lang="kn">ಕ್ಕ</span>.',
    'Press <kbd>F9</kbd> to switch between Kannada and English, then save as Word (.docx) or copy the Unicode text anywhere.',
]

how_to_type = {
    "slug": "how-to-type-in-nudi-kannada",
    "title": "How to Type in Nudi Kannada – Step-by-Step Nudi Keyboard Guide",
    "description": "Learn how to type Kannada in Nudi: which key gives which letter, how vowel signs and ottaksharas work, and practise right on the page. Free, works on any keyboard.",
    "crumb": "How to Type in Nudi Kannada",
    "h1": "How to Type in Nudi Kannada",
    "tagline": "ನುಡಿಯಲ್ಲಿ ಕನ್ನಡ ಟೈಪ್ ಮಾಡುವುದು ಹೇಗೆ?",
    "lead": "Nudi is the Kannada keyboard layout standardised by the Government of Karnataka and KAGAPA. Once you know three simple rules – <strong>consonant keys</strong>, <strong>vowel-sign keys</strong> and the <strong><kbd>f</kbd> key for ottaksharas</strong> – you can type any Kannada word on an ordinary English keyboard.",
    "howto": ("How to type Kannada using the Nudi keyboard", HOW_TO_TYPE_STEPS),
    "scripts": TYPE_BOX_SCRIPTS,
    "body": f"""
        <section>
            <h2>How to type Kannada in Nudi – 6 steps</h2>
            <ol class="steps">
                {"".join(f"<li>{s}</li>" for s in HOW_TO_TYPE_STEPS)}
            </ol>
        </section>
{try_box()}
        <section>
            <h2>Rule 1: each consonant key types a full letter</h2>
            <p>In Nudi every consonant key already carries the vowel “a”. Lower-case and upper-case keys give related letters – usually the unaspirated and aspirated pair: <kbd>k</kbd> → {kn("ಕ")}, <kbd>K</kbd> → {kn("ಖ")}; <kbd>t</kbd> → {kn("ತ")}, <kbd>T</kbd> → {kn("ಥ")}. The full chart is below and on the <a href="/kannada-nudi-typing-keyboard-chart/">printable Nudi keyboard chart</a>.</p>
        </section>

        <section>
            <h2>Rule 2: vowel keys after a consonant add the vowel sign</h2>
            <p>Typed on their own, vowel keys give independent vowels (<kbd>a</kbd> → {kn("ಅ")}, <kbd>A</kbd> → {kn("ಆ")}). Typed right after a consonant they add the matching vowel sign (<span lang="kn">ಕಾಗುಣಿತ</span>):</p>
            {example_table([("kA", "ಕಾ", "ka + aa"), ("ki", "ಕಿ", "ka + i"), ("kI", "ಕೀ", "ka + ii"), ("ku", "ಕು", "ka + u"), ("ke", "ಕೆ", "ka + e"), ("kY", "ಕೈ", "ka + ai"), ("ko", "ಕೊ", "ka + o"), ("kV", "ಕೌ", "ka + au"), ("kM", "ಕಂ", "anusvara"), ("kH", "ಕಃ", "visarga")])}
        </section>

        <section>
            <h2>Rule 3: press <kbd>f</kbd> to make ottaksharas</h2>
            <p>The <kbd>f</kbd> key adds a halant ({kn("್")}) to the consonant you just typed. The next consonant then joins underneath as an ottakshara (vattakshara):</p>
            {example_table([("knfnw", "ಕನ್ನಡ", "Kannada"), ("nmsfkAr", "ನಮಸ್ಕಾರ", "Namaskara"), ("amfm", "ಅಮ್ಮ", "Mother"), ("hNfNu", "ಹಣ್ಣು", "Fruit"), ("vidfyA", "ವಿದ್ಯಾ", "Education")])}
            <p>Use upper-case <kbd>F</kbd> instead when you want a visible half letter instead of a joined conjunct.</p>
        </section>

        <section>
            <h2>Common words to practise</h2>
            {example_table([("nuwi", "ನುಡಿ", "Language"), ("beMgLUru", "ಬೆಂಗಳೂರು", "Bengaluru"), ("mYsUru", "ಮೈಸೂರು", "Mysuru"), ("SAle", "ಶಾಲೆ", "School"), ("sMtOx", "ಸಂತೋಷ", "Happiness"), ("DnfyvAd", "ಧನ್ಯವಾದ", "Thank you"), ("krfnAqk", "ಕರ್ನಾಟಕ", "Karnataka")])}
            <p>See <a href="/how-to-type-karnataka-in-nudi/">how to type Karnataka (ಕರ್ನಾಟಕ)</a> and <a href="/how-to-type-ksha-in-kannada-nudi/">how to type ಕ್ಷ, ಜ್ಞ and ಶ್ರೀ</a> for the trickier letters.</p>
        </section>

        <section>
            <h2>Nudi keyboard chart</h2>
            <p>Keys are case-sensitive. Keep this chart open while you learn, or <a href="/kannada-nudi-typing-keyboard-chart/">download it as a PDF</a>.</p>
            {letter_chart()}
        </section>

        <section>
            <h2>How to type Kannada in the Nudi software</h2>
            <p>The KAGAPA Nudi software (Nudi 4.0, 5.0, 6.0 and 6.1) uses this same layout. If you already learned on Nudi software you can type in Kannada Nudi Editor without relearning anything – and the text you produce is Unicode, so it also displays correctly on phones and websites. To type directly inside Word, Excel or a browser on Windows, install the <a href="{STORE_WINDOWS}">Windows app</a> and press <kbd>F9</kbd> to turn on Global Mode. See <a href="/nudi-kannada-typing-in-ms-word/">Nudi typing in MS Word</a>.</p>
        </section>
{related([("/learn-nudi-kannada-typing/", "Learn Nudi typing – lessons and drills"), ("/kannada-nudi-typing-letters-in-english/", "All Nudi letters with English keys"), ("/type-kannada-using-english-keyboard/", "Type Kannada using an English keyboard"), ("/baraha-kannada-typing/", "Prefer phonetic? Baraha typing")])}""",
    "faqs": [
        ("How do I type in Nudi on a normal English keyboard?", "No special keyboard is needed. Open the <a href=\"/\">editor</a>, keep the layout on Nudi/KGP and type using the key chart – for example <kbd>k</kbd><kbd>n</kbd><kbd>f</kbd><kbd>n</kbd><kbd>w</kbd> gives ಕನ್ನಡ."),
        ("Why does f not type the letter f?", "In the Nudi layout <kbd>f</kbd> is the halant (್) key used to join consonants. Kannada has no ‘f’ sound of its own; ಫ is on <kbd>P</kbd> and ಫ಼ on <kbd>P</kbd><kbd>X</kbd>."),
        ("How do I switch between Kannada and English?", "Press <kbd>F9</kbd>, or click the KAN / ENG switch in the toolbar."),
        ("Is Nudi different from Baraha?", "Yes. Nudi is a fixed key-per-letter layout (like a typewriter); Baraha is phonetic – you spell the sound, e.g. ‘kannaDa’. Kannada Nudi Editor supports both; see <a href=\"/baraha-kannada-typing/\">Baraha typing</a>."),
        ("Do I need to install Nudi fonts?", "No. The editor produces Unicode Kannada, which every modern computer and phone can display without extra fonts."),
    ],
}

# ---------------------------------------------------------------------------
karnataka = {
    "slug": "how-to-type-karnataka-in-nudi",
    "title": "How to Type Karnataka (ಕರ್ನಾಟಕ) in Nudi – Keys k r f n A q k",
    "description": "Type ಕರ್ನಾಟಕ in Kannada Nudi with the keys k r f n A q k. Step-by-step explanation of the arkavattu (ರ್), copy-ready ಕರ್ನಾಟಕ text and the old Nudi font code PÀ£ÁðlPÀ.",
    "crumb": "How to Type Karnataka in Nudi",
    "h1": "How to Type Karnataka (ಕರ್ನಾಟಕ) in Nudi",
    "tagline": "ನುಡಿಯಲ್ಲಿ ‘ಕರ್ನಾಟಕ’ ಎಂದು ಬರೆಯುವುದು ಹೇಗೆ?",
    "lead": f"To write <span lang=\"kn\">ಕರ್ನಾಟಕ</span> in Nudi, press {keys_html('krfnAqk')}. The only tricky part is <span lang=\"kn\">ರ್ನಾ</span>: you type the <strong>ರ</strong> first and join it with <kbd>f</kbd>, and Nudi draws it as the arkavattu (<span lang=\"kn\">ರ್</span> hook) after <span lang=\"kn\">ನಾ</span>.",
    "scripts": TYPE_BOX_SCRIPTS,
    "howto": ("How to type ಕರ್ನಾಟಕ in Nudi", [
        "Press k – you get ಕ.", "Press r – you get ಕರ.", "Press f – the halant joins ರ to the next letter: ಕರ್.",
        "Press n – you get ಕರ್ನ (shown with the arkavattu).", "Press A – the vowel sign makes it ಕರ್ನಾ.",
        "Press q – adds ಟ: ಕರ್ನಾಟ.", "Press k – adds ಕ: ಕರ್ನಾಟಕ."]),
    "body": f"""
        <section>
            <h2>Keys for ಕರ್ನಾಟಕ, one at a time</h2>
            <div class="table-scroll"><table class="examples"><thead><tr><th>Press</th><th>Screen shows</th><th>What happened</th></tr></thead><tbody>
                <tr><td>{typed('k', 'ಕ')}</td><td lang="kn">ಕ</td><td>Consonant ka</td></tr>
                <tr><td>{typed('r', 'ರ')}</td><td lang="kn">ಕರ</td><td>Consonant ra</td></tr>
                <tr><td>{keys_html('f')}</td><td lang="kn">ಕರ್</td><td>Halant – ರ will join the next letter</td></tr>
                <tr><td>{keys_html('n')}</td><td lang="kn">ಕರ್ನ</td><td>ರ + ನ becomes the arkavattu form</td></tr>
                <tr><td>{keys_html('A')}</td><td lang="kn">ಕರ್ನಾ</td><td>Vowel sign aa on ನ</td></tr>
                <tr><td>{keys_html('q')}</td><td lang="kn">ಕರ್ನಾಟ</td><td>Consonant ṭa (retroflex) is on <kbd>q</kbd></td></tr>
                <tr><td>{typed('krfnAqk', 'ಕರ್ನಾಟಕ')}</td><td lang="kn">ಕರ್ನಾಟಕ</td><td>Done</td></tr>
            </tbody></table></div>
            <p>Remember: <kbd>q</kbd> is <span lang="kn">ಟ</span> and <kbd>t</kbd> is <span lang="kn">ತ</span>. Typing <kbd>t</kbd> by mistake gives <span lang="kn">ಕರ್ನಾತಕ</span>.</p>
        </section>
{try_box(placeholder="Type k r f n A q k here")}
        <section>
            <h2>Why is ರ typed before ನ?</h2>
            <p>Nudi follows the spoken order of sounds: kar-naa-ṭa-ka. The <span lang="kn">ರ್</span> is pronounced before <span lang="kn">ನಾ</span>, so you type it first. Kannada script then writes this “half ra” as the <strong>arkavattu</strong> – the small hook drawn after the next letter – which is why it appears to the right of <span lang="kn">ನಾ</span> on screen. The same rule gives <span lang="kn">ಧರ್ಮ</span>, <span lang="kn">ಸೂರ್ಯ</span> and <span lang="kn">ಕಾರ್ಯ</span>.</p>
            {example_table([("Drfm", "ಧರ್ಮ", "Dharma"), ("sUrfy", "ಸೂರ್ಯ", "Sun"), ("kArfy", "ಕಾರ್ಯ", "Work"), ("srfkAr", "ಸರ್ಕಾರ", "Government"), ("vrfx", "ವರ್ಷ", "Year")])}
        </section>

        <section>
            <h2>Karnataka phrases in Nudi</h2>
            {example_table([("krfnAqk srfkAr", "ಕರ್ನಾಟಕ ಸರ್ಕಾರ", "Government of Karnataka"), ("krfnAqk rAjfy", "ಕರ್ನಾಟಕ ರಾಜ್ಯ", "State of Karnataka"), ("knfnw nAwu", "ಕನ್ನಡ ನಾಡು", "Kannada land"), ("krfnAqk rAjfyOtfsv", "ಕರ್ನಾಟಕ ರಾಜ್ಯೋತ್ಸವ", "Karnataka Rajyotsava")])}
        </section>

        <section>
            <h2>Copy ಕರ್ನಾಟಕ (Unicode)</h2>
            <p class="copy-line"><span lang="kn" class="big">ಕರ್ನಾಟಕ</span> <button type="button" class="btn btn-small" onclick="navigator.clipboard.writeText('ಕರ್ನಾಟಕ').then(()=>this.textContent='Copied')">Copy</button></p>
            <p>This is standard Unicode text – paste it into Word, WhatsApp, email or a website and it displays correctly everywhere.</p>
        </section>

        <section>
            <h2>Karnataka in the old Nudi (ASCII) font</h2>
            <p>Old Nudi 4.0 documents store Kannada as English letters drawn by a special font. In that format ಕರ್ನಾಟಕ is saved as <code>PÀ£ÁðlPÀ</code> and only looks right with a font such as “Nudi 01 e”. Paste text like this into the <a href="/nudi-ascii-to-unicode/">Nudi ASCII to Unicode converter</a> to turn it into normal Kannada, or use the <a href="/nudi-6-to-nudi-4-converter/">Nudi 6 to Nudi 4 converter</a> to go the other way.</p>
        </section>
{related([("/how-to-type-in-nudi-kannada/", "How to type in Nudi Kannada"), ("/how-to-type-ksha-in-kannada-nudi/", "How to type ಕ್ಷ, ಜ್ಞ and ಶ್ರೀ"), ("/kannada-nudi-typing-keyboard-chart/", "Nudi keyboard chart (PDF)")])}""",
    "faqs": [
        ("How do I type Karnataka in Kannada Nudi?", "Press <kbd>k</kbd> <kbd>r</kbd> <kbd>f</kbd> <kbd>n</kbd> <kbd>A</kbd> <kbd>q</kbd> <kbd>k</kbd>. The <kbd>f</kbd> joins ರ to ನ, which Kannada writes as the arkavattu."),
        ("How do I write Karnataka in Nudi with the arkavattu?", "Type the ರ before the letter it sits on and press <kbd>f</kbd> after it: ರ + <kbd>f</kbd> + ನಾ. You never type the arkavattu hook separately."),
        ("Why do I get ಕರ್ನಾತಕ instead of ಕರ್ನಾಟಕ?", "You pressed <kbd>t</kbd> (ತ). The retroflex ಟ is on <kbd>q</kbd>."),
        ("How do I type ಕರ್ನಾಟಕ on my phone?", "Install <a href=\"" + STORE_PLAY + "\">Kannada Nudi Keyboard for Android</a> or the <a href=\"" + STORE_APPLE + "\">iPhone app</a>, choose the Nudi layout and use the same keys."),
    ],
}

# ---------------------------------------------------------------------------
ksha = {
    "slug": "how-to-type-ksha-in-kannada-nudi",
    "title": "How to Type ಕ್ಷ (Ksha) in Kannada Nudi – plus ಜ್ಞ, ಶ್ರೀ, ಋ and More",
    "description": "Type ಕ್ಷ in Nudi with k f x. Keys for every tricky Kannada letter: ಜ್ಞ, ಶ್ರೀ, ತ್ರ, ಕೃ, ಋ, ಙ, ಞ, ಱ, ೞ, arkavattu and the visible halant, with examples to practise.",
    "crumb": "How to Type Ksha in Nudi",
    "h1": "How to Type ಕ್ಷ (Ksha) in Kannada Nudi",
    "tagline": "ನುಡಿಯಲ್ಲಿ ಕ್ಷ, ಜ್ಞ, ಶ್ರೀ ಟೈಪ್ ಮಾಡುವುದು ಹೇಗೆ?",
    "lead": f"<span lang=\"kn\">ಕ್ಷ</span> is not a separate key in Nudi – it is <span lang=\"kn\">ಕ</span> + halant + <span lang=\"kn\">ಷ</span>. Press {keys_html('kfx')}: <kbd>k</kbd> for <span lang=\"kn\">ಕ</span>, <kbd>f</kbd> to join, and <kbd>x</kbd> for <span lang=\"kn\">ಷ</span>.",
    "scripts": TYPE_BOX_SCRIPTS,
    "body": f"""
        <section>
            <h2>Words with ಕ್ಷ</h2>
            {example_table([("kfx", "ಕ್ಷ", "ksha"), ("kfxme", "ಕ್ಷಮೆ", "Forgiveness"), ("lkfxfmi", "ಲಕ್ಷ್ಮಿ", "Lakshmi"), ("prIkfxe", "ಪರೀಕ್ಷೆ", "Exam"), ("rkfxNe", "ರಕ್ಷಣೆ", "Protection"), ("kfxEtfr", "ಕ್ಷೇತ್ರ", "Field"), ("SikfxN", "ಶಿಕ್ಷಣ", "Education")])}
            <p>Add any vowel sign after <kbd>x</kbd> as usual: <kbd>k</kbd><kbd>f</kbd><kbd>x</kbd><kbd>A</kbd> → <span lang="kn">ಕ್ಷಾ</span>, <kbd>k</kbd><kbd>f</kbd><kbd>x</kbd><kbd>i</kbd> → <span lang="kn">ಕ್ಷಿ</span>.</p>
        </section>
{try_box(placeholder="Type k f x m e for ಕ್ಷಮೆ")}
        <section>
            <h2>ಜ್ಞ (jña)</h2>
            <p><span lang="kn">ಜ್ಞ</span> = <span lang="kn">ಜ</span> + halant + <span lang="kn">ಞ</span>. <span lang="kn">ಞ</span> is on lower-case <kbd>z</kbd>.</p>
            {example_table([("jfz", "ಜ್ಞ", "jña"), ("jfzAn", "ಜ್ಞಾನ", "Knowledge"), ("vijfzAn", "ವಿಜ್ಞಾನ", "Science"), ("AjfzA", "ಆಜ್ಞಾ", "Command")])}
        </section>

        <section>
            <h2>ಶ್ರೀ, ತ್ರ, ಸ್ತ್ರೀ – ra as an ottakshara</h2>
            <p>When <span lang="kn">ರ</span> comes after another consonant it becomes the small ra-ottu. Type the first consonant, <kbd>f</kbd>, then <kbd>r</kbd>:</p>
            {example_table([("SfrI", "ಶ್ರೀ", "Shri"), ("ptfr", "ಪತ್ರ", "Letter"), ("kfrm", "ಕ್ರಮ", "Order"), ("sftfrI", "ಸ್ತ್ರೀ", "Woman"), ("rAxfqfr", "ರಾಷ್ಟ್ರ", "Nation")])}
        </section>

        <section>
            <h2>Arkavattu (ರ್ before a consonant)</h2>
            <p>When <span lang="kn">ರ್</span> comes <em>before</em> a consonant, type <kbd>r</kbd><kbd>f</kbd> first and then the consonant. Kannada draws it as the arkavattu hook after the letter:</p>
            {example_table([("krfnAqk", "ಕರ್ನಾಟಕ", "Karnataka"), ("Drfm", "ಧರ್ಮ", "Dharma"), ("arfT", "ಅರ್ಥ", "Meaning"), ("kIrfti", "ಕೀರ್ತಿ", "Fame")])}
        </section>

        <section>
            <h2>ಋ and the ೃ vowel sign</h2>
            {example_table([("R", "ಋ", "Independent ru"), ("Rxi", "ಋಷಿ", "Sage"), ("kRxfN", "ಕೃಷ್ಣ", "Krishna"), ("kRpe", "ಕೃಪೆ", "Grace"), ("dRxfqi", "ದೃಷ್ಟಿ", "Sight")])}
        </section>

        <section>
            <h2>Rare and archaic letters</h2>
            <div class="table-scroll"><table class="examples"><thead><tr><th>Keys</th><th>Letter</th><th>Name</th></tr></thead><tbody>
                <tr><td>{typed('Z', 'ಙ')}</td><td lang="kn">ಙ</td><td>nga</td></tr>
                <tr><td>{typed('J', 'ಝ')}</td><td lang="kn">ಝ</td><td>jha</td></tr>
                <tr><td>{typed('z', 'ಞ')}</td><td lang="kn">ಞ</td><td>nya</td></tr>
                <tr><td>{typed('N', 'ಣ')}</td><td lang="kn">ಣ</td><td>ṇa (retroflex)</td></tr>
                <tr><td>{typed('L', 'ಳ')}</td><td lang="kn">ಳ</td><td>ḷa</td></tr>
                <tr><td>{typed('rX', 'ಱ')}</td><td lang="kn">ಱ</td><td>Shakata repha (archaic ra)</td></tr>
                <tr><td>{typed('LX', 'ೞ')}</td><td lang="kn">ೞ</td><td>Archaic ḻa</td></tr>
                <tr><td>{typed('RX', 'ೠ')}</td><td lang="kn">ೠ</td><td>Long ṝ</td></tr>
                <tr><td>{typed('jX', 'ಜ಼')}</td><td lang="kn">ಜ಼</td><td>za (with nukta)</td></tr>
                <tr><td>{typed('PX', 'ಫ಼')}</td><td lang="kn">ಫ಼</td><td>fa (with nukta)</td></tr>
                <tr><td>{typed('aM', 'ಅಂ')}</td><td lang="kn">ಅಂ</td><td>Anusvara</td></tr>
                <tr><td>{typed('aH', 'ಅಃ')}</td><td lang="kn">ಅಃ</td><td>Visarga</td></tr>
            </tbody></table></div>
        </section>

        <section>
            <h2>Visible halant: <kbd>f</kbd> vs <kbd>F</kbd></h2>
            <p>Lower-case <kbd>f</kbd> joins the two consonants into a conjunct (<span lang="kn">ಕ್ಕ</span>). Upper-case <kbd>F</kbd> keeps the halant visible and does not form a conjunct – useful for English words and names written in Kannada. At the end of a word, <kbd>f</kbd> alone gives the halant: <kbd>b</kbd><kbd>s</kbd><kbd>f</kbd> → <span lang="kn">ಬಸ್</span>.</p>
        </section>
{related([("/how-to-type-karnataka-in-nudi/", "How to type ಕರ್ನಾಟಕ in Nudi"), ("/kannada-nudi-typing-letters-in-english/", "All Nudi letters with English keys"), ("/kannada-nudi-typing-keyboard-chart/", "Nudi keyboard chart (PDF)")])}""",
    "faqs": [
        ("How do I type ksha (ಕ್ಷ) in Nudi?", "Press <kbd>k</kbd> <kbd>f</kbd> <kbd>x</kbd>. <kbd>k</kbd> is ಕ, <kbd>f</kbd> is the halant that joins consonants, and <kbd>x</kbd> is ಷ."),
        ("Which key is ಷ in Nudi?", "Lower-case <kbd>x</kbd> is ಷ. Upper-case <kbd>S</kbd> is ಶ and lower-case <kbd>s</kbd> is ಸ."),
        ("How do I type ಶ್ರೀ in Nudi?", "Press <kbd>S</kbd> <kbd>f</kbd> <kbd>r</kbd> <kbd>I</kbd>."),
        ("How do I type ಜ್ಞಾನ in Nudi?", "Press <kbd>j</kbd> <kbd>f</kbd> <kbd>z</kbd> <kbd>A</kbd> <kbd>n</kbd>."),
        ("How do I type ಲಕ್ಷ್ಮಿ in Nudi?", "Press <kbd>l</kbd> <kbd>k</kbd> <kbd>f</kbd> <kbd>x</kbd> <kbd>f</kbd> <kbd>m</kbd> <kbd>i</kbd> – two <kbd>f</kbd> presses for the double ottakshara."),
    ],
}

# ---------------------------------------------------------------------------
LESSONS = [
    ("Lesson 1 – Home row consonants", "Start with the keys your fingers rest on. Type each word three times.",
     [("jl", "ಜಲ"), ("dL", "ದಳ"), ("hgl", "ಹಗಲ"), ("gj", "ಗಜ"), ("jg", "ಜಗ"), ("kl", "ಕಲ")]),
    ("Lesson 2 – Vowel signs", "Add <kbd>A</kbd>, <kbd>i</kbd>, <kbd>u</kbd>, <kbd>e</kbd>, <kbd>o</kbd> after a consonant.",
     [("kAl", "ಕಾಲ"), ("giri", "ಗಿರಿ"), ("guru", "ಗುರು"), ("mne", "ಮನೆ"), ("hos", "ಹೊಸ"), ("SAle", "ಶಾಲೆ")]),
    ("Lesson 3 – Long vowels and ಐ, ಔ, ಂ", "Shift gives the long vowel: <kbd>I</kbd>, <kbd>U</kbd>, <kbd>E</kbd>, <kbd>O</kbd>.",
     [("nIru", "ನೀರು"), ("hUvu", "ಹೂವು"), ("bEku", "ಬೇಕು"), ("mYsUru", "ಮೈಸೂರು"), ("gVrv", "ಗೌರವ"), ("aMgwi", "ಅಂಗಡಿ")]),
    ("Lesson 4 – Ottaksharas with f", "Press <kbd>f</kbd> between consonants to join them.",
     [("amfm", "ಅಮ್ಮ"), ("apfp", "ಅಪ್ಪ"), ("knfnw", "ಕನ್ನಡ"), ("hNfNu", "ಹಣ್ಣು"), ("pusftk", "ಪುಸ್ತಕ"), ("nmsfkAr", "ನಮಸ್ಕಾರ")]),
    ("Lesson 5 – Arkavattu and special letters", "ರ್ before a consonant, plus ಕ್ಷ and ಜ್ಞ.",
     [("krfnAqk", "ಕರ್ನಾಟಕ"), ("srfkAr", "ಸರ್ಕಾರ"), ("kfxme", "ಕ್ಷಮೆ"), ("jfzAn", "ಜ್ಞಾನ"), ("SfrI", "ಶ್ರೀ"), ("kRxfN", "ಕೃಷ್ಣ")]),
]

learn = {
    "slug": "learn-nudi-kannada-typing",
    "title": "Learn Nudi Kannada Typing – Free Lessons, Drills and Practice Box",
    "description": "How to learn Nudi Kannada typing: five short lessons from the home row to ottaksharas and arkavattu, a practice box on the page and tips to build speed. Free.",
    "crumb": "Learn Nudi Kannada Typing",
    "h1": "Learn Nudi Kannada Typing",
    "tagline": "ನುಡಿ ಕನ್ನಡ ಟೈಪಿಂಗ್ ಕಲಿಯಿರಿ – ಉಚಿತವಾಗಿ",
    "lead": "Nudi typing is required for many Karnataka government jobs and is the fastest way to type Kannada once learned. Work through these five lessons – each takes about 15 minutes – using the practice box below or the full <a href=\"/\">online editor</a>.",
    "scripts": TYPE_BOX_SCRIPTS,
    "body": f"""
        <section>
            <h2>Before you start</h2>
            <ul class="features">
                <li><strong>Hand position:</strong> left fingers on <kbd>a</kbd> <kbd>s</kbd> <kbd>d</kbd> <kbd>f</kbd>, right fingers on <kbd>j</kbd> <kbd>k</kbd> <kbd>l</kbd> <kbd>;</kbd> – the same as English touch typing.</li>
                <li><strong>Shift matters:</strong> lower-case and upper-case keys give different letters (<kbd>t</kbd> = <span lang="kn">ತ</span>, <kbd>T</kbd> = <span lang="kn">ಥ</span>).</li>
                <li><strong>Think in sounds:</strong> type consonant first, then its vowel sign, exactly in the order you say it.</li>
                <li>Keep the <a href="/kannada-nudi-typing-keyboard-chart/">Nudi keyboard chart</a> printed next to you for the first week.</li>
            </ul>
        </section>
{try_box(placeholder="Practise the lesson words here", title="Practice box")}
        {"".join(f'''<section>
            <h2>{t}</h2>
            <p>{intro}</p>
            {example_table(rows, meaning=False)}
        </section>''' for t, intro, rows in LESSONS)}

        <section>
            <h2>Building speed</h2>
            <ol class="steps">
                <li>Accuracy first: type slowly without looking at the keyboard until you make no mistakes.</li>
                <li>Copy a paragraph from a Kannada newspaper every day in the <a href="/">editor</a>.</li>
                <li>Learn the common clusters as single movements: <kbd>n</kbd><kbd>f</kbd><kbd>n</kbd> (<span lang="kn">ನ್ನ</span>), <kbd>t</kbd><kbd>f</kbd><kbd>t</kbd> (<span lang="kn">ತ್ತ</span>), <kbd>s</kbd><kbd>f</kbd><kbd>t</kbd> (<span lang="kn">ಸ್ತ</span>).</li>
                <li>Most typing tests count 25–30 correct words per minute as a pass. Practise with a timer once you are comfortable.</li>
            </ol>
        </section>
{related([("/how-to-type-in-nudi-kannada/", "How to type in Nudi – the three rules"), ("/kannada-nudi-typing-letters-in-english/", "Nudi letters and kagunita in English"), ("/how-to-type-ksha-in-kannada-nudi/", "Special letters: ಕ್ಷ, ಜ್ಞ, ಶ್ರೀ")])}""",
    "faqs": [
        ("How long does it take to learn Nudi typing?", "Most people learn the layout in a week of daily practice and reach 20–30 words per minute within one to two months."),
        ("Is Nudi typing used in government exams?", "Yes. Karnataka government offices use the Nudi (KGP) layout, and Kannada typing tests are typically taken on it. Check your exam notification for its exact rules."),
        ("Can I practise Nudi typing without installing software?", "Yes. Use the practice box on this page or the full <a href=\"/\">online editor</a>; both run in the browser."),
        ("Should I learn Nudi or Baraha?", "Learn Nudi if you need it for a job or exam, or want maximum speed. <a href=\"/baraha-kannada-typing/\">Baraha</a> is easier to start with if you already think in English spellings."),
    ],
}

# ---------------------------------------------------------------------------
english_kb = {
    "slug": "type-kannada-using-english-keyboard",
    "title": "How to Type Kannada Using an English Keyboard – Nudi & Baraha, Free",
    "description": "Type Kannada on a normal English (QWERTY) keyboard with no extra hardware. Use the Nudi layout or phonetic Baraha typing online, on Windows, Mac, Linux, Android and iPhone.",
    "crumb": "Type Kannada Using an English Keyboard",
    "h1": "How to Type Kannada Using an English Keyboard",
    "tagline": "ಇಂಗ್ಲಿಷ್ ಕೀಬೋರ್ಡ್‌ನಲ್ಲಿ ಕನ್ನಡ ಟೈಪ್ ಮಾಡಿ",
    "lead": "You don't need a special Kannada keyboard. Kannada Nudi Editor maps Kannada letters onto the keys you already have, in two ways: the standard <strong>Nudi</strong> layout and phonetic <strong>Baraha</strong> typing.",
    "scripts": TYPE_BOX_SCRIPTS,
    "body": f"""
        <section>
            <h2>Option 1: Nudi layout (one key per letter)</h2>
            <p>Each English key gives a fixed Kannada letter – <kbd>k</kbd> = <span lang="kn">ಕ</span>, <kbd>n</kbd> = <span lang="kn">ನ</span>, <kbd>w</kbd> = <span lang="kn">ಡ</span> – and <kbd>f</kbd> joins consonants. It is the official Karnataka layout and the fastest once learned.</p>
            {example_table([("knfnw", "ಕನ್ನಡ", "Kannada"), ("nmsfkAr", "ನಮಸ್ಕಾರ", "Hello")])}
            <p><a href="/how-to-type-in-nudi-kannada/">How to type in Nudi →</a></p>
        </section>

        <section>
            <h2>Option 2: Baraha phonetic typing (spell the sound)</h2>
            <p>Type how the word sounds in English letters: <code>kannaDa</code> → <span lang="kn">ಕನ್ನಡ</span>, <code>namaskaara</code> → <span lang="kn">ನಮಸ್ಕಾರ</span>. Easiest if you have never learned a Kannada layout. Switch the editor's Layout menu to <strong>Baraha</strong>. <a href="/baraha-kannada-typing/">Baraha typing guide →</a></p>
        </section>
{try_box(title="Try Nudi typing on your English keyboard")}
        <section>
            <h2>Where can I type Kannada?</h2>
            <div class="platforms">
                <div class="platform"><h3>In the browser</h3><p>Open the <a href="/">online editor</a> – works on any computer, saves as Word (.docx).</p></div>
                <div class="platform"><h3>Windows</h3><p>The <a href="{STORE_WINDOWS}">Microsoft Store app</a> adds Global Mode – press <kbd>F9</kbd> to type Kannada in Word, Excel, Chrome or any app.</p></div>
                <div class="platform"><h3>Linux</h3><p><a href="{STORE_SNAP}">Snap Store</a>: <code>sudo snap install kannadanudi</code>. <kbd>F9</kbd> works in every app through IBus.</p></div>
                <div class="platform"><h3>Android</h3><p><a href="{STORE_PLAY}">Kannada Nudi Keyboard</a> – Nudi and Baraha layouts in WhatsApp, Gmail and all apps.</p></div>
                <div class="platform"><h3>iPhone, iPad, Mac</h3><p><a href="{STORE_APPLE}">Kannada Nudi Editor on the App Store</a>.</p></div>
            </div>
        </section>

        <section>
            <h2>Nudi key chart</h2>
            {letter_chart()}
        </section>
{related([("/kannada-nudi-typing-keyboard-chart/", "Printable Nudi keyboard chart"), ("/learn-nudi-kannada-typing/", "Learn Nudi typing step by step"), ("/kannada-typing-software-for-windows-11/", "Kannada typing on Windows 11")])}""",
    "faqs": [
        ("Can I type Kannada using an English keyboard?", "Yes. Every key on a normal QWERTY keyboard is mapped to a Kannada letter in the Nudi layout, or you can spell words phonetically with Baraha. No stickers or new keyboard needed."),
        ("How do I type in Kannada keyboard on a laptop?", "Open the <a href=\"/\">editor</a> in your browser and start typing – the Nudi layout is on by default. To type in other apps on Windows, install the <a href=\"" + STORE_WINDOWS + "\">Windows app</a> and press <kbd>F9</kbd>."),
        ("Which is easier, Nudi or Baraha?", "Baraha is easier on day one because you spell sounds. Nudi takes a few days to learn but is faster and is the government standard."),
        ("Does it work offline?", "The desktop and mobile apps work fully offline. The web editor needs internet only to load the page."),
    ],
}

PAGES = [how_to_type, karnataka, ksha, learn, english_kb]
