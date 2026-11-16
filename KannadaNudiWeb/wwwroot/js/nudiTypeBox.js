/*
 * Minimal Nudi / KGP typing for the "Try it" boxes on the static landing pages.
 * Mirrors the Nudi branch of Services/KannadaKeyboardEngine.cs (consonants, vowels and
 * vowel signs, f/F halant, rX / LX / RX / jX / PX / KX specials). Punctuation is passed
 * through unchanged. Any <textarea data-nudi-type> on the page is wired up automatically.
 */
(function () {
    'use strict';

    const CONS = {
        k: 'ಕ', K: 'ಖ', g: 'ಗ', G: 'ಘ', Z: 'ಙ', c: 'ಚ', C: 'ಛ', j: 'ಜ', J: 'ಝ', z: 'ಞ',
        q: 'ಟ', Q: 'ಠ', w: 'ಡ', W: 'ಢ', N: 'ಣ', t: 'ತ', T: 'ಥ', d: 'ದ', D: 'ಧ', n: 'ನ',
        p: 'ಪ', P: 'ಫ', b: 'ಬ', B: 'ಭ', m: 'ಮ', y: 'ಯ', r: 'ರ', l: 'ಲ', v: 'ವ',
        S: 'ಶ', x: 'ಷ', s: 'ಸ', h: 'ಹ', L: 'ಳ'
    };
    const VOWELS = {
        a: 'ಅ', A: 'ಆ', i: 'ಇ', I: 'ಈ', u: 'ಉ', U: 'ಊ', R: 'ಋ', e: 'ಎ', E: 'ಏ',
        Y: 'ಐ', o: 'ಒ', O: 'ಓ', V: 'ಔ', M: 'ಂ', H: 'ಃ'
    };
    const MATRAS = {
        a: '', A: 'ಾ', i: 'ಿ', I: 'ೀ', u: 'ು', U: 'ೂ', R: 'ೃ', e: 'ೆ', E: 'ೇ',
        Y: 'ೈ', o: 'ೊ', O: 'ೋ', V: 'ೌ', M: 'ಂ', H: 'ಃ'
    };
    const SPECIALS = { rX: 'ಱ', LX: 'ೞ', RX: 'ೠ', jX: 'ಜ಼', PX: 'ಫ಼', KX: 'ಖ಼' };
    const SPECIAL_MATRAS = { RX: 'ೄ' };
    const SPECIAL_CONS = new Set(['ಱ', 'ೞ', 'ಜ಼', 'ಫ಼', 'ಖ಼']);

    function matraContext(buf) {
        if (!buf) return false;
        const last = buf[buf.length - 1];
        if (CONS[last]) return true;
        if (last === 'f' || last === 'F' || VOWELS[last]) return false;
        return buf.length >= 2 && SPECIAL_CONS.has(SPECIALS[buf.slice(-2)]);
    }

    /** Returns [text, charsToDelete] for one key, updating state.buf. */
    function translate(state, key) {
        const buf = state.buf;
        if (buf) {
            const seq = buf[buf.length - 1] + key;
            if (SPECIALS[seq]) {
                const m = matraContext(buf.slice(0, -1));
                state.buf += key;
                return [m && SPECIAL_MATRAS[seq] ? SPECIAL_MATRAS[seq] : SPECIALS[seq], 1];
            }
        }
        if ((key === 'f' || key === 'F') && buf && CONS[buf[buf.length - 1]]) {
            state.buf += key;
            return [key === 'f' ? '್' : '್‍', 0];
        }
        if (VOWELS[key]) {
            const m = matraContext(buf);
            state.buf += key;
            return [m ? MATRAS[key] : VOWELS[key], 0];
        }
        state.buf += key;
        if (state.buf.length > 16) state.buf = state.buf.slice(-8);
        return [CONS[key] || key, 0];
    }

    function insert(el, text, del) {
        const start = Math.max(0, el.selectionStart - del);
        const end = el.selectionEnd;
        el.setRangeText(text, start, end, 'end');
        el.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function attach(el) {
        const state = { buf: '', on: true };
        const toggle = el.closest('.try-box') && el.closest('.try-box').querySelector('[data-nudi-toggle]');

        function setMode(on) {
            state.on = on;
            state.buf = '';
            if (toggle) {
                toggle.textContent = on ? 'KAN' : 'ENG';
                toggle.setAttribute('aria-pressed', String(on));
            }
        }
        if (toggle) toggle.addEventListener('click', () => { setMode(!state.on); el.focus(); });

        el.addEventListener('keydown', e => {
            if (e.key === 'F9') { e.preventDefault(); setMode(!state.on); return; }
            if (e.key === 'Backspace' || e.key === 'Delete' || e.key.startsWith('Arrow') ||
                e.key === 'Enter' || e.key === 'Home' || e.key === 'End') state.buf = '';
        });
        el.addEventListener('mousedown', () => { state.buf = ''; });

        el.addEventListener('beforeinput', e => {
            if (!state.on || e.inputType !== 'insertText' || !e.data || e.data.length !== 1) return;
            if (e.isComposing) return;
            e.preventDefault();
            const [text, del] = translate(state, e.data);
            insert(el, text, del);
        });
    }

    // Node (tests / page generator): expose the key logic only.
    if (typeof document === 'undefined') {
        if (typeof module !== 'undefined') module.exports = { translate };
        return;
    }

    function init() {
        document.querySelectorAll('textarea[data-nudi-type]').forEach(attach);
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
