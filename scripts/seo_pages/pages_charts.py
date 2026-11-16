"""Printable chart pages: keyboard chart (PDF) and letters-in-English / kagunita."""
from common import (CONSONANTS, VOWELS, example_table, kagunita_table, keyboard_diagram, kn,
                    letter_chart, print_button, related, esc)

chart = {
    "slug": "kannada-nudi-typing-keyboard-chart",
    "title": "Kannada Nudi Typing Keyboard Chart (PDF) – All Letters with Keys",
    "description": "Printable Kannada Nudi keyboard chart for computer typing: every vowel, consonant, vowel sign and ottakshara with its key. View online or save as PDF – free.",
    "crumb": "Nudi Keyboard Chart",
    "h1": "Kannada Nudi Typing Keyboard Chart",
    "tagline": "ಕನ್ನಡ ನುಡಿ ಕೀಲಿಮಣೆ ಚಾರ್ಟ್ – PDF",
    "lead": "The complete Nudi / KGP keyboard chart for computer Kannada typing: which key gives which letter, the vowel-sign (kagunita) keys and how to join ottaksharas. Print it or save it as a PDF to keep beside your keyboard.",
    "hero_cta": print_button(),
    "body": f"""
        <section>
            <h2>Nudi keyboard layout</h2>
            <p>This is how the Nudi letters sit on a standard computer (QWERTY) keyboard. The bottom letter on each key is typed directly; the top one with <kbd>Shift</kbd>.</p>
            {keyboard_diagram()}
        </section>

        <section>
            <h2>Nudi letters chart – key for every letter</h2>
            {letter_chart()}
        </section>

        <section>
            <h2>Vowel sign keys (kagunita)</h2>
            <p>Type the consonant, then the vowel key. Example for <span lang="kn">ಕ</span> – the same keys work after every consonant:</p>
            {kagunita_table([CONSONANTS[0]], caption="Kagunita of ಕ with Nudi keys")}
            <p>See the <a href="/kannada-nudi-typing-letters-in-english/">full kagunita chart for all 34 consonants</a>.</p>
        </section>

        <section>
            <h2>Ottakshara and special keys</h2>
            {example_table([("kfk", "ಕ್ಕ", "f joins two consonants"), ("kfx", "ಕ್ಷ", "ksha"), ("jfz", "ಜ್ಞ", "jña"), ("SfrI", "ಶ್ರೀ", "ra-ottu"), ("krfnAqk", "ಕರ್ನಾಟಕ", "arkavattu"), ("kR", "ಕೃ", "ru vowel sign")])}
        </section>

        <section class="no-print">
            <h2>Save the chart as a PDF</h2>
            <ol class="steps">
                <li>Click <strong>Download as PDF / Print</strong> at the top of this page (or press <kbd>Ctrl</kbd>+<kbd>P</kbd>).</li>
                <li>Choose <strong>Save as PDF</strong> as the printer.</li>
                <li>Click <strong>Save</strong>. The page is laid out to print the charts only, without menus.</li>
            </ol>
        </section>
{related([("/how-to-type-in-nudi-kannada/", "How to type in Nudi Kannada"), ("/learn-nudi-kannada-typing/", "Learn Nudi typing – lessons"), ("/how-to-type-ksha-in-kannada-nudi/", "How to type ಕ್ಷ, ಜ್ಞ, ಶ್ರೀ")])}""",
    "faqs": [
        ("Is there a Kannada Nudi keyboard chart PDF?", "Yes – click “Download as PDF / Print” on this page and choose “Save as PDF”. The print layout contains just the keyboard diagram and letter charts."),
        ("Is this chart the same as Nudi 6.1 and KGP?", "Yes. It follows the Nudi / KGP layout standardised by the Government of Karnataka and KAGAPA, used by Nudi 4.0, 5.0, 6.0 and 6.1."),
        ("Which key is the halant (ottu) in Nudi?", "<kbd>f</kbd>. Type it between two consonants to join them, e.g. <kbd>k</kbd><kbd>f</kbd><kbd>k</kbd> gives ಕ್ಕ."),
        ("Which key is ಝ in Nudi?", "Upper-case <kbd>J</kbd>. Lower-case <kbd>j</kbd> is ಜ, so <kbd>J</kbd><kbd>A</kbd> gives ಝಾ."),
    ],
}

# ---------------------------------------------------------------------------
_vowel_rows = "".join(
    f'<tr><td lang="kn" class="big">{esc(g)}</td><td><kbd>{esc(k)}</kbd></td></tr>' for g, k in VOWELS)
_cons_rows = "".join(
    f'<tr><td lang="kn" class="big">{esc(g)}</td><td><kbd>{esc(k)}</kbd></td></tr>' for g, k in CONSONANTS)

letters = {
    "slug": "kannada-nudi-typing-letters-in-english",
    "title": "Kannada Nudi Typing Letters in English – Full Kagunita Chart (PDF)",
    "description": "Every Kannada Nudi typing letter with its English key: vowels, consonants and the complete kagunita (ಕ ಕಾ ಕಿ ಕೀ…) for all 34 consonants. Printable – save as PDF free.",
    "crumb": "Nudi Typing Letters in English",
    "h1": "Kannada Nudi Typing Letters in English",
    "tagline": "ನುಡಿ ಅಕ್ಷರಗಳು ಮತ್ತು ಕಾಗುಣಿತ – ಇಂಗ್ಲಿಷ್ ಕೀಲಿಗಳೊಂದಿಗೆ",
    "lead": "A complete list of Nudi typing letters with the English key you press for each – first the alphabet (<span lang=\"kn\">ಅಕ್ಷರಮಾಲೆ</span>), then the full <span lang=\"kn\">ಕಾಗುಣಿತ</span> table for every consonant. Print it or save it as a PDF.",
    "hero_cta": print_button(),
    "body": f"""
        <section>
            <h2>Vowels (ಸ್ವರಗಳು) and their keys</h2>
            <div class="two-col">
                <div class="table-scroll"><table class="examples"><thead><tr><th>Letter</th><th>English key</th></tr></thead><tbody>{_vowel_rows}</tbody></table></div>
                <div>
                    <p>Vowel keys typed <strong>alone</strong> give the independent vowel. Typed <strong>after a consonant</strong> they add the vowel sign instead – that is how the kagunita below is formed.</p>
                    <p>Short and long pairs differ only by <kbd>Shift</kbd>: <kbd>i</kbd> {kn("ಇ")} / <kbd>I</kbd> {kn("ಈ")}, <kbd>u</kbd> {kn("ಉ")} / <kbd>U</kbd> {kn("ಊ")}, <kbd>e</kbd> {kn("ಎ")} / <kbd>E</kbd> {kn("ಏ")}, <kbd>o</kbd> {kn("ಒ")} / <kbd>O</kbd> {kn("ಓ")}.</p>
                </div>
            </div>
        </section>

        <section>
            <h2>Consonants (ವ್ಯಂಜನಗಳು) and their keys</h2>
            <div class="table-scroll cols-table"><table class="examples"><thead><tr><th>Letter</th><th>English key</th></tr></thead><tbody>{_cons_rows}</tbody></table></div>
        </section>

        <section>
            <h2>Complete kagunita chart with Nudi keys</h2>
            <p>Each cell shows the letter and the keys to type it. The same vowel keys apply to every consonant.</p>
            {kagunita_table()}
        </section>

        <section>
            <h2>Ottaksharas</h2>
            <p>Join any two consonants with <kbd>f</kbd>: <em>first consonant + <kbd>f</kbd> + second consonant</em>.</p>
            {example_table([("kfk", "ಕ್ಕ"), ("gfg", "ಗ್ಗ"), ("tft", "ತ್ತ"), ("nfn", "ನ್ನ"), ("mfm", "ಮ್ಮ"), ("lfl", "ಲ್ಲ"), ("sft", "ಸ್ತ"), ("kfx", "ಕ್ಷ"), ("jfz", "ಜ್ಞ"), ("tfr", "ತ್ರ")], meaning=False)}
        </section>
{related([("/kannada-nudi-typing-keyboard-chart/", "Nudi keyboard layout chart (PDF)"), ("/how-to-type-in-nudi-kannada/", "How to type in Nudi Kannada"), ("/learn-nudi-kannada-typing/", "Learn Nudi typing – lessons")])}""",
    "faqs": [
        ("How do I download the Nudi letters PDF in English?", "Click “Download as PDF / Print” at the top and choose “Save as PDF” in the print dialog."),
        ("What is the English key for ಳ in Nudi?", "Upper-case <kbd>L</kbd>. Lower-case <kbd>l</kbd> is ಲ."),
        ("Which English key is ಡ in Nudi?", "<kbd>w</kbd> is ಡ and <kbd>W</kbd> is ಢ. The retroflex ಟ / ಠ are on <kbd>q</kbd> / <kbd>Q</kbd>."),
        ("How do I type ಅಂ and ಅಃ?", "<kbd>a</kbd><kbd>M</kbd> gives ಅಂ and <kbd>a</kbd><kbd>H</kbd> gives ಅಃ. After a consonant, <kbd>M</kbd> and <kbd>H</kbd> add ಂ and ಃ."),
    ],
}

PAGES = [chart, letters]
