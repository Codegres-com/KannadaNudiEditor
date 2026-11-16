/*
 * Text converter widget for the landing pages: <section class="tool" data-direction="a2u|u2a">.
 * a2u = old Nudi (ASCII) -> Unicode Kannada, u2a = Unicode Kannada -> Nudi (ASCII).
 * Uses js/nudiConverter.js. Everything runs locally; no text is sent anywhere.
 */
(function () {
    'use strict';

    function init(tool) {
        const dir = tool.dataset.direction === 'u2a' ? 'u2a' : 'a2u';
        const $ = sel => tool.querySelector(sel);
        const input = $('[data-in]');
        const output = $('[data-out]');
        const status = $('[data-status]');
        const mixed = $('[data-mixed]');
        const fontToggle = $('[data-font-toggle]');
        const sample = tool.dataset.sample || '';

        let converter = null;
        let timer = 0;

        function say(msg) { if (status) status.textContent = msg; }

        function run() {
            if (!converter) return;
            const text = input.value;
            try {
                output.value = dir === 'a2u'
                    ? converter.asciiToUnicode(text, { mixed: !!(mixed && mixed.checked) })
                    : converter.unicodeToAscii(text);
                say(text ? `Converted ${text.length.toLocaleString()} characters.` : '');
            } catch (err) {
                say('Conversion failed: ' + err.message);
            }
        }

        function schedule() {
            clearTimeout(timer);
            timer = setTimeout(run, input.value.length > 20000 ? 300 : 60);
        }

        input.addEventListener('input', schedule);
        if (mixed) mixed.addEventListener('change', run);

        if (fontToggle) {
            const target = dir === 'a2u' ? input : output;
            const apply = () => target.classList.toggle('nudi-font', fontToggle.checked);
            fontToggle.addEventListener('change', apply);
            apply();
        }

        tool.addEventListener('click', async e => {
            const btn = e.target.closest('button[data-action]');
            if (!btn) return;
            switch (btn.dataset.action) {
                case 'sample':
                    input.value = sample;
                    run();
                    break;
                case 'clear':
                    input.value = '';
                    output.value = '';
                    say('');
                    input.focus();
                    break;
                case 'copy':
                    if (!output.value) { say('Nothing to copy yet.'); break; }
                    try {
                        await navigator.clipboard.writeText(output.value);
                    } catch {
                        output.select();
                        document.execCommand('copy');
                    }
                    say('Copied to clipboard.');
                    break;
                case 'download': {
                    if (!output.value) { say('Nothing to download yet.'); break; }
                    const blob = new Blob([output.value], { type: 'text/plain;charset=utf-8' });
                    const a = document.createElement('a');
                    a.href = URL.createObjectURL(blob);
                    a.download = dir === 'a2u' ? 'kannada-unicode.txt' : 'kannada-nudi-ascii.txt';
                    document.body.appendChild(a);
                    a.click();
                    a.remove();
                    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
                    break;
                }
            }
        });

        const file = $('[data-file]');
        if (file) {
            file.addEventListener('change', () => {
                const f = file.files && file.files[0];
                if (!f) return;
                if (/\.docx?$/i.test(f.name)) {
                    say('Word files keep their formatting in the editor – see "Convert a Word file" below.');
                    file.value = '';
                    return;
                }
                f.arrayBuffer().then(buf => {
                    // Prefer UTF-8; legacy Nudi .txt files saved as ANSI are Windows-1252.
                    let text;
                    try { text = new TextDecoder('utf-8', { fatal: true }).decode(buf); }
                    catch { text = new TextDecoder('windows-1252').decode(buf); }
                    input.value = text.replace(/^﻿/, '');
                    run();
                });
                file.value = '';
            });
        }

        say('Loading converter…');
        NudiConverter.load('/Resources/').then(c => {
            converter = c;
            say('');
            if (input.value) run();
        }).catch(err => say('Could not load the converter: ' + err.message));
    }

    function start() { document.querySelectorAll('.tool[data-direction]').forEach(init); }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
    else start();
})();
