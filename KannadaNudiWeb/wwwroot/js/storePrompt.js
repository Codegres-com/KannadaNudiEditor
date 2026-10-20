// Suggests the Microsoft Store app to visitors using Chrome, Edge or Firefox on Windows.
// Never shown inside the desktop (Electron) builds. Dismissal is remembered for SNOOZE_DAYS.
// Append ?storeprompt=1 to the URL to force it for testing.
(function () {
    const STORE_URL = 'https://apps.microsoft.com/detail/9mxxjp296jlq';
    const STORAGE_KEY = 'nudi.storePrompt.dismissedAt';
    const SNOOZE_DAYS = 14;
    const SHOW_DELAY_MS = 4000;

    function forced() {
        return /[?&]storeprompt=1\b/.test(location.search);
    }

    function isEligibleBrowser() {
        const ua = navigator.userAgent;
        if (!/Windows NT/.test(ua)) return false;
        if (/Electron\//.test(ua) || window.nudiDesktop) return false;
        // Edge ("Edg/"), Chrome ("Chrome/") and Firefox ("Firefox/"); skip Opera.
        if (/OPR\//.test(ua)) return false;
        return /Edg\/|Chrome\/|Firefox\//.test(ua);
    }

    function isSnoozed() {
        try {
            const at = parseInt(localStorage.getItem(STORAGE_KEY) || '0', 10);
            return at > 0 && Date.now() - at < SNOOZE_DAYS * 86400000;
        } catch {
            return false;
        }
    }

    function snooze() {
        try { localStorage.setItem(STORAGE_KEY, String(Date.now())); } catch { /* storage unavailable */ }
    }

    function show() {
        if (document.getElementById('store-prompt')) return;

        const card = document.createElement('div');
        card.id = 'store-prompt';
        card.className = 'store-prompt';
        card.setAttribute('role', 'dialog');
        card.setAttribute('aria-labelledby', 'store-prompt-title');
        card.innerHTML = `
            <button type="button" class="btn-close store-prompt-close" aria-label="Close"></button>
            <div class="store-prompt-body">
                <img src="nudi_logo.png" alt="" width="48" height="48" />
                <div>
                    <div id="store-prompt-title" class="store-prompt-title">Get Kannada Nudi for Windows</div>
                    <div class="store-prompt-text">Type Kannada in Word, Excel and any app with <kbd>F9</kbd> Global Mode. Free and works offline.</div>
                </div>
            </div>
            <div class="store-prompt-actions">
                <button type="button" class="btn btn-link btn-sm text-muted store-prompt-later">Not now</button>
                <a class="btn btn-primary btn-sm store-prompt-get" href="${STORE_URL}" target="_blank" rel="noopener">
                    <span class="bi bi-microsoft"></span> Get it from Microsoft Store
                </a>
            </div>`;

        const close = () => {
            snooze();
            card.classList.remove('show');
            setTimeout(() => card.remove(), 250);
        };
        card.querySelector('.store-prompt-close').addEventListener('click', close);
        card.querySelector('.store-prompt-later').addEventListener('click', close);
        card.querySelector('.store-prompt-get').addEventListener('click', close);
        document.addEventListener('keydown', function onKey(e) {
            if (e.key === 'Escape' && document.getElementById('store-prompt')) {
                close();
                document.removeEventListener('keydown', onKey);
            }
        });

        document.body.appendChild(card);
        requestAnimationFrame(() => card.classList.add('show'));
    }

    function init() {
        if (!forced() && (!isEligibleBrowser() || isSnoozed())) return;
        setTimeout(show, SHOW_DELAY_MS);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
