/*
 * Nudi (legacy ASCII) <-> Unicode Kannada converter, in plain JavaScript.
 *
 * A line-by-line port of Helpers/Conversion/AsciiToUnicode.cs, UnicodeToAscii.cs and
 * MixedContentConversion.cs (ConvertMixedAsciiToUnicode) so the static landing pages can
 * convert text in the browser without loading the Blazor app. Both use the same mapping
 * files in /Resources/, so keep the two implementations in step when changing either.
 *
 * Usage (browser):
 *   const c = await NudiConverter.load('/Resources/');
 *   c.asciiToUnicode('PÀ£ÀßqÀ')            // -> 'ಕನ್ನಡ'
 *   c.asciiToUnicode(text, { mixed: true }) // keep English words unchanged
 *   c.unicodeToAscii('ಕನ್ನಡ')              // -> 'PÀ£ÀßqÀ'
 *
 * Usage (Node, for tests): NudiConverter.fromJson(a2uJson, u2aJson)
 */
(function (root) {
    'use strict';

    const NUKTA = '಼';

    const isKannadaOrJoiner = c => (c >= 'ಀ' && c <= '೿') || c === '‌' || c === '‍';
    const isKannadaConsonant = c => (c >= 'ಕ' && c <= 'ಹ') || c === 'ೞ';
    const first = (s, fallback) => (s ? s[0] : fallback);
    const charSet = arr => new Set((arr || []).filter(Boolean).map(s => s[0]));
    const has = (obj, k) => Object.prototype.hasOwnProperty.call(obj, k);

    // ---------------------------------------------------------------
    // Config (FileConversionCore.cs ConversionConfig.From)
    // ---------------------------------------------------------------
    function buildConfig(rootJson, isA2U) {
        const meta = rootJson.meta || {};
        let maxToken = meta.maxTokenLength > 0 ? meta.maxTokenLength : 4;
        if (maxToken > 16) maxToken = 16;

        const mapping = rootJson.mapping || {};
        const buckets = Array.from({ length: maxToken + 1 }, () => []);
        let maxKeyLen = 0;
        for (const key of Object.keys(mapping)) {
            const len = key.length;
            if (len <= 0 || len > maxToken) continue;
            if (len > maxKeyLen) maxKeyLen = len;
            buckets[len].push(key);
        }

        const brokenCases = {};
        for (const [k, v] of Object.entries(rootJson.brokenCases || {})) {
            const m = new Map();
            for (const [mk, mv] of Object.entries((v && v.mapping) || {})) {
                if (mk && mv) m.set(mk[0], mv[0]);
            }
            brokenCases[k] = { value: (v && v.value) || '', mapping: m };
        }

        const digitA2U = new Map();
        for (const [k, v] of Object.entries(rootJson.numbersMapping || {})) {
            if (k.length === 1) digitA2U.set(k, v || '');
        }

        const digitU2A = new Map();
        const kd = rootJson.kannadaDigits, ad = rootJson.asciiDigits;
        if (kd && ad && kd.length === ad.length) {
            kd.forEach((d, i) => { if (d && ad[i]) digitU2A.set(d[0], ad[i][0]); });
        }

        const rx = rootJson.u2aRegex || {};
        let p4 = rx.uniRephWithoutZwj || '';
        if (!p4.trim() || /PASTE_THE_EXACT/i.test(p4)) {
            p4 = '\\u0CB0\\u0CCD\\u0CCD(?<baseVattaCons>\\u200D?[\\u0C95-\\u0CB9\\u0CDE])' +
                '(?<restChain>(?:\\u0CCD\\u200D?[\\u0C95-\\u0CB9\\u0CDE])*)(?<dv>[\\u0CBE-\\u0CCC\\u0C82\\u0C83]{0,2})';
        }

        let prevChars = charSet(rootJson.unicodePrevValueChars);
        if (prevChars.size === 0) {
            prevChars = new Set('ಾಿೀುೂೃೄೆೇೈೊೋೌ್ಂಃ‍');
        }

        return {
            maxToken,
            maxKeyLen,
            buckets,
            zwj: first(meta.zwj, '‍'),
            zwnj: first(meta.zwnj, '‌'),
            halant: first(meta.halant, '್'),
            asciiHalant: first(meta.asciiHalantChar, 'ï'),
            asciiNukta: first(meta.asciiNuktaChar, 'ó'),
            asciiArkavattuChar: first(meta.asciiArkavattuChar, 'ð'),
            consonantStart: new Set(rootJson.asciiConsonantStartChars || ''),
            joinBeforeNoSpace: new Set(rootJson.asciiJoinCharsBeforeNoSpace || ''),
            mapping,
            dependentVowels: charSet(rootJson.dependentVowels),
            ignore: charSet(rootJson.ignoreList),
            vattaksharagalu: rootJson.vattaksharagalu || {},
            arkavattu: rootJson.asciiArkavattu || {},
            brokenCases,
            postFixups: (rootJson.postFixups || []).filter(Boolean).map(x => [x.from || '', x.to || '']),
            enableA2UExtras: isA2U,
            prevChars,
            unicodeVowels: new Set(rootJson.unicodeVowels || []),
            digitA2U,
            digitU2A,
            rxVowelAnusvara: new RegExp(rx.uniVowelPlusAnusvaraVisarga ||
                '(?<v>[\\u0C85-\\u0C94\\u0C8E-\\u0C90\\u0CE0])(?<av>[\\u0C82\\u0C83])', 'g'),
            rxConsVowel: new RegExp(rx.uniConsonantPlusVowel ||
                '(?<base>[\\u0C95-\\u0CB9\\u0CDE])(?<dv>[\\u0CBE-\\u0CCC\\u0C82\\u0C83]{1,2})(?!\\u0CCD|\\u200D)', 'g'),
            rxVattakshara: new RegExp(rx.uniVattakshara ||
                '(?<base>[\\u0C95-\\u0CB9\\u0CDE])(?<chain>(?:\\u0CCD\\u200D?[\\u0C95-\\u0CB9\\u0CDE])+)(?<dv>[\\u0CBE-\\u0CCC\\u0C82\\u0C83]{0,2})', 'g'),
            rxReph: new RegExp(p4, 'g')
        };
    }

    // A small mutable char buffer standing in for StringBuilder.
    class Buf {
        constructor() { this.a = []; }
        get length() { return this.a.length; }
        at(i) { return this.a[i]; }
        last() { return this.a[this.a.length - 1]; }
        push(s) { for (const ch of s.split('')) this.a.push(ch); }
        insert(i, s) { this.a.splice(i, 0, ...s.split('')); }
        set(i, ch) { this.a[i] = ch; }
        pop() { this.a.pop(); }
        toString() { return this.a.join(''); }
    }

    // ---------------------------------------------------------------
    // ASCII -> Unicode (AsciiToUnicode.cs)
    // ---------------------------------------------------------------
    function asciiToUnicode(input, cfg) {
        if (!input) return '';

        input = moveNuktaAfterVowelSigns(input, cfg.asciiNukta);
        let s = preNormalizeAscii(input, cfg);
        if (cfg.enableA2UExtras) s = preInsertZwnj(s, cfg);

        let mapped = processStream(s, cfg);
        if (cfg.enableA2UExtras) mapped = postProcessClusters(mapped, cfg);
        mapped = applyFixupsKannadaRuns(mapped, cfg.postFixups);
        mapped = normalizeJoinersAfterHalant(mapped, cfg);
        return mapped.normalize('NFC');
    }

    function processStream(text, cfg) {
        const sb = new Buf();
        const n = text.length;
        const maxToken = cfg.maxKeyLen > 0 ? Math.min(cfg.maxToken, cfg.maxKeyLen) : cfg.maxToken;
        let i = 0;

        while (i < n) {
            if (cfg.ignore.has(text[i])) { i++; continue; }

            const top = Math.min(maxToken, n - i);
            let matched = false;
            for (let len = top; len >= 1 && !matched; len--) {
                if (len >= cfg.buckets.length) continue;
                const sub = text.substr(i, len);
                if (cfg.buckets[len].length && has(cfg.mapping, sub) && sub.length === len) {
                    if (cfg.enableA2UExtras && sb.length > 0 && sb.last() === cfg.halant) sb.push(cfg.zwj);
                    sb.push(cfg.mapping[sub]);
                    i += len;
                    matched = true;
                }
            }
            if (matched) continue;

            const ch = text[i];
            if (ch === cfg.asciiNukta) {
                const start = findSyllableStart(sb, cfg);
                if (start >= 0) sb.insert(start + 1, NUKTA);
            } else if (has(cfg.arkavattu, ch)) {
                const ra = cfg.arkavattu[ch];
                const start = findSyllableStart(sb, cfg);
                if (start >= 0) sb.insert(start, ra + cfg.halant);
                else sb.push(ra + cfg.halant);
            } else if (has(cfg.vattaksharagalu, ch)) {
                const base = cfg.vattaksharagalu[ch];
                while (sb.length > 0 && (sb.last() === cfg.zwj || sb.last() === cfg.zwnj)) sb.pop();
                if (sb.length > 0 && sb.last() === cfg.halant) sb.push(base + cfg.halant);
                else sb.push(cfg.halant + base);
            } else if (has(cfg.brokenCases, ch)) {
                applyBrokenCase(sb, cfg.brokenCases[ch], cfg);
            } else if (cfg.digitA2U.has(ch)) {
                sb.push(cfg.digitA2U.get(ch));
            } else if (ch === cfg.asciiHalant) {
                sb.push(cfg.halant);
            } else {
                sb.push(ch);
            }
            i++;
        }
        return sb.toString();
    }

    function applyBrokenCase(sb, bc, cfg) {
        if (sb.length === 0) return;
        let j = sb.length;
        while (j >= 2 && isKannadaConsonant(sb.at(j - 1)) && sb.at(j - 2) === cfg.halant) j -= 2;
        while (j > 0 && sb.at(j - 1) === NUKTA) j--;
        if (j === 0) return;
        const v = sb.at(j - 1);
        if (cfg.dependentVowels.has(v) && bc.mapping.has(v)) sb.set(j - 1, bc.mapping.get(v));
    }

    function findSyllableStart(sb, cfg) {
        let j = sb.length;
        while (j > 0) {
            const c = sb.at(j - 1);
            if (cfg.dependentVowels.has(c) || c === 'ಂ' || c === 'ಃ' ||
                c === cfg.zwj || c === cfg.zwnj || c === cfg.halant || c === NUKTA) {
                j--;
            } else if (isKannadaConsonant(c) && j >= 2 && sb.at(j - 2) === cfg.halant) {
                j -= 2;
            } else break;
        }
        return (j > 0 && isKannadaConsonant(sb.at(j - 1))) ? j - 1 : -1;
    }

    function moveNuktaAfterVowelSigns(input, nukta) {
        if (input.indexOf(nukta) < 0) return input;
        const n = nukta.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        return input.replace(new RegExp(n + '([À-Ë]+)', 'g'), (_, g) => g + nukta);
    }

    function preNormalizeAscii(input, cfg) {
        if (cfg.joinBeforeNoSpace.size === 0) return input;
        let out = '';
        for (let i = 0; i < input.length; i++) {
            const ch = input[i];
            if ((ch === ' ' || ch === '\t') && i + 1 < input.length) {
                const prev = i > 0 ? input[i - 1] : '\0';
                const next = input[i + 1];
                const prevK = prev >= '\u0080' || prev === cfg.asciiHalant || cfg.consonantStart.has(prev);
                const nextK = next >= '\u0080' || cfg.consonantStart.has(next) || cfg.joinBeforeNoSpace.has(next);
                if (prevK && nextK && cfg.joinBeforeNoSpace.has(next)) continue;
            }
            out += ch;
        }
        return out;
    }

    function preInsertZwnj(input, cfg) {
        if (input.indexOf(cfg.asciiHalant) < 0 || cfg.consonantStart.size === 0) return input;
        let out = '';
        for (let i = 0; i < input.length; i++) {
            const ch = input[i];
            out += ch;
            if (ch === cfg.asciiHalant && i + 1 < input.length) {
                const next = input[i + 1];
                if (next <= '\u007F' && /[A-Za-z0-9_-]/.test(next)) continue;
                if (cfg.consonantStart.has(next)) out += cfg.zwnj;
            }
        }
        return out;
    }

    function mapKannadaRuns(text, fn) {
        let out = '', i = 0;
        while (i < text.length) {
            if (!isKannadaOrJoiner(text[i])) { out += text[i++]; continue; }
            const start = i;
            while (i < text.length && isKannadaOrJoiner(text[i])) i++;
            out += fn(text.substring(start, i));
        }
        return out;
    }

    function applyFixupsKannadaRuns(text, fixups) {
        if (!text || fixups.length === 0) return text;
        return mapKannadaRuns(text, run => {
            for (const [from, to] of fixups) if (from) run = run.split(from).join(to);
            return run;
        });
    }

    function postProcessClusters(text, cfg) {
        return mapKannadaRuns(text, run => {
            let out = '', i = 0;
            while (i < run.length) {
                const ch = run[i];
                if (!cfg.dependentVowels.has(ch)) { out += ch; i++; continue; }
                if (i + 1 < run.length && run[i + 1] === cfg.halant) {
                    let j = i + 1, chain = '';
                    while (j < run.length && run[j] === cfg.halant) {
                        if (j + 1 >= run.length) break;
                        const cons = run[j + 1];
                        if (!isKannadaConsonant(cons)) break;
                        chain += cfg.halant + cons;
                        j += 2;
                    }
                    out += chain + ch;
                    i = j;
                    continue;
                }
                out += ch;
                i++;
            }
            return out;
        });
    }

    function normalizeJoinersAfterHalant(text, cfg) {
        if (!text || text.indexOf(cfg.halant) < 0) return text;
        let out = '', i = 0;
        while (i < text.length) {
            const c = text[i];
            out += c;
            i++;
            if (c !== cfg.halant) continue;
            let j = i;
            while (j < text.length && (text[j] === cfg.zwj || text[j] === cfg.zwnj)) j++;
            if (j === i) continue;
            if (j < text.length && isKannadaConsonant(text[j])) out += cfg.zwnj;
            i = j;
        }
        return out;
    }

    // ---------------------------------------------------------------
    // Mixed English + Nudi plain text (MixedContentConversion.cs)
    // ---------------------------------------------------------------
    const ENGLISH_WORD = /(?<![A-Za-z\u0080-ÿ])[a-z]{5,}(?![A-Za-z\u0080-ÿ])/;

    function classifyToken(token) {
        let asciiLetters = 0, glyphs = 0;
        for (const c of token) {
            if (c >= 'ಀ' && c <= '೿') return 'other';
            if (c > 'ÿ') return 'other';
            if (c >= '¡') glyphs++;
            else if (c < '\u0080' && /[A-Za-z]/.test(c)) asciiLetters++;
        }
        const hasDigit = /[0-9]/.test(token);
        if (glyphs >= 2 || (glyphs === 1 && asciiLetters <= 3 && !hasDigit)) return 'legacy';
        if (glyphs === 1) return 'other';
        return asciiLetters > 0 ? 'plain' : 'other';
    }

    function mixedAsciiToUnicode(text, cfg) {
        if (!text) return text || '';
        const tokens = [...text.matchAll(/\S+/g)];
        if (tokens.length === 0) return text;
        const kind = tokens.map(t => classifyToken(t[0]));
        for (let i = 0; i < kind.length; i++) {
            if (kind[i] !== 'plain') continue;
            const prevL = i > 0 && kind[i - 1] === 'legacy';
            const nextL = i + 1 < kind.length && kind[i + 1] === 'legacy';
            if (prevL && nextL && !ENGLISH_WORD.test(tokens[i][0])) kind[i] = 'context';
        }
        const isL = k => k === 'legacy' || k === 'context';
        let out = '', pos = 0, k = 0;
        while (k < tokens.length) {
            if (!isL(kind[k])) { k++; continue; }
            const start = tokens[k].index;
            let m = k;
            while (m + 1 < tokens.length && isL(kind[m + 1])) m++;
            const end = tokens[m].index + tokens[m][0].length;
            out += text.substring(pos, start) + asciiToUnicode(text.substring(start, end), cfg);
            pos = end;
            k = m + 1;
        }
        return out + text.substring(pos);
    }

    // ---------------------------------------------------------------
    // Unicode -> ASCII (UnicodeToAscii.cs)
    // ---------------------------------------------------------------
    function unicodeToAscii(input, cfg) {
        if (!input) return input || '';
        let out = input.split(' ').map(w => (w ? unicodeWordToAscii(w, cfg) : '')).join(' ');
        out = applyBrokenCasesU2A(out, cfg);
        for (const [from, to] of cfg.postFixups) if (from) out = out.split(from).join(to);
        return deerghaHandle(out, cfg);
    }

    const mapOrSelf = (s, cfg) => (has(cfg.mapping, s) ? cfg.mapping[s] : s);

    function splitLetters(txt, prev) {
        const out = [];
        for (const c of txt) {
            const lastL = out.length ? out[out.length - 1] : '';
            const attach = prev.has(c) ||
                (out.length > 0 && isKannadaOrJoiner(c) && lastL.length > 0 &&
                    lastL[lastL.length - 1] === '್' && isKannadaConsonant(c));
            if (out.length > 0 && isKannadaOrJoiner(c) && attach) out[out.length - 1] += c;
            else out.push(c);
        }
        return out;
    }

    function unicodeWordToAscii(word, cfg) {
        let s = splitLetters(word, cfg.prevChars).map(l => rearrangeAndReplace(l, cfg)).join('');
        s = s.split('‌').join('');
        if (cfg.digitU2A.size > 0) s = [...s].map(ch => cfg.digitU2A.get(ch) || ch).join('');
        return s;
    }

    function rearrangeAndReplace(inp, cfg) {
        if (!inp) return inp;
        if (has(cfg.mapping, inp)) return cfg.mapping[inp];
        if (cfg.unicodeVowels.has(inp)) return mapOrSelf(inp, cfg);

        inp = inp.replace(cfg.rxVowelAnusvara, (...a) => {
            const g = a[a.length - 1];
            return mapOrSelf(g.v, cfg) + mapOrSelf(g.av, cfg);
        });
        inp = inp.replace(cfg.rxReph, (...a) => {
            const g = a[a.length - 1];
            let b = g.baseVattaCons || '';
            if (b && b[0] === cfg.zwj) b = b.substring(1);
            return substituteAscii(b, g.dv || '', g.restChain || '', cfg.asciiArkavattuChar, cfg);
        });
        inp = inp.replace(cfg.rxVattakshara, (...a) => {
            const g = a[a.length - 1];
            return substituteAscii(g.base, g.dv || '', g.chain || '', null, cfg);
        });
        inp = inp.replace(cfg.rxConsVowel, (...a) => {
            const g = a[a.length - 1];
            return substituteAscii(g.base, g.dv || '', '', null, cfg);
        });
        return mapOrSelf(inp, cfg);
    }

    function substituteAscii(base, dv, chain, append, cfg) {
        let out = '';
        const d0 = dv.length > 0 ? dv[0] : null;
        const d1 = dv.length > 1 ? dv[1] : null;
        const d0AV = d0 === 'ಂ' || d0 === 'ಃ';

        if (d0 === null) out += mapOrSelf(base, cfg);
        else if (!d0AV) out += mapOrSelf(base + d0, cfg);
        else out += mapOrSelf(base, cfg);

        for (let i = 0; i < chain.length; i++) {
            const ch = chain[i];
            if (ch === cfg.zwj) continue;
            if (ch === cfg.halant && i + 1 < chain.length) {
                let next = chain[i + 1];
                let hasZwj = false;
                if (next === cfg.zwj && i + 2 < chain.length) {
                    hasZwj = true;
                    next = chain[i + 2];
                    i++;
                }
                out += mapVatta(next, hasZwj, cfg);
                i++;
            }
        }
        if (d0AV) out += mapOrSelf(d0, cfg);
        if (d1 !== null) out += mapOrSelf(d1, cfg);
        if (append) out += append;
        return out;
    }

    function mapVatta(cons, hasZwj, cfg) {
        const kz = cfg.halant + cfg.zwj + cons, kp = cfg.halant + cons;
        if (hasZwj && has(cfg.mapping, kz)) return cfg.mapping[kz];
        if (has(cfg.mapping, kp)) return cfg.mapping[kp];
        if (has(cfg.vattaksharagalu, cons)) return cfg.vattaksharagalu[cons];
        return mapOrSelf(cons, cfg);
    }

    function applyBrokenCasesU2A(ascii, cfg) {
        if (!ascii || Object.keys(cfg.brokenCases).length === 0) return ascii;
        const sb = [];
        for (const ch of ascii) {
            const bc = cfg.brokenCases[ch];
            if (!bc) { sb.push(ch); continue; }
            if (sb.length > 0 && bc.mapping.has(sb[sb.length - 1])) {
                sb[sb.length - 1] = bc.mapping.get(sb[sb.length - 1]);
                continue;
            }
            if (bc.value) sb.push(bc.value);
        }
        return sb.join('');
    }

    function deerghaHandle(ascii, cfg) {
        if (!ascii) return ascii;
        const dvTokens = new Set();
        for (const dv of 'ಾಿೀುೂೃೄೆೇೈೊೋೌ') {
            if (has(cfg.mapping, dv) && cfg.mapping[dv].length === 1) {
                const t = cfg.mapping[dv];
                if (t !== cfg.asciiHalant && t !== cfg.asciiArkavattuChar) dvTokens.add(t);
            }
        }
        if (dvTokens.size === 0) return ascii;
        const V = c => cfg.consonantStart.has(c), D = c => dvTokens.has(c);
        const arr = ascii.split('');
        for (let i = 0; i < arr.length; i++) {
            if (arr[i] === ' ') continue;
            for (const span of [3, 2, 1]) {
                if (i + span < arr.length && D(arr[i]) && arr.slice(i + 1, i + 1 + span).every(V)) {
                    const dv = arr[i];
                    for (let k = 0; k < span; k++) arr[i + k] = arr[i + k + 1];
                    arr[i + span] = dv;
                    i += span;
                    break;
                }
            }
        }
        return arr.join('');
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------
    function fromJson(a2uJson, u2aJson) {
        const a2u = a2uJson ? buildConfig(a2uJson, true) : null;
        const u2a = u2aJson ? buildConfig(u2aJson, false) : null;
        return {
            asciiToUnicode(text, opts) {
                if (!a2u) throw new Error('ASCII→Unicode mapping not loaded');
                return opts && opts.mixed ? mixedAsciiToUnicode(text, a2u) : asciiToUnicode(text, a2u);
            },
            unicodeToAscii(text) {
                if (!u2a) throw new Error('Unicode→ASCII mapping not loaded');
                return unicodeToAscii(text, u2a);
            }
        };
    }

    async function load(base) {
        base = base || '/Resources/';
        const get = f => fetch(base + f).then(r => {
            if (!r.ok) throw new Error('Could not load ' + f);
            return r.json();
        });
        const [a, u] = await Promise.all([get('AsciiToUnicodeMapping.json'), get('UnicodeToAsciiMapping.json')]);
        return fromJson(a, u);
    }

    const api = { load, fromJson };
    if (typeof module !== 'undefined' && module.exports) module.exports = api;
    else root.NudiConverter = api;
})(typeof self !== 'undefined' ? self : this);
