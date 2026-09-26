/**
 * TrackPH theme toggle — persists to localStorage.
 * layout.html calls initTheme() on DOMContentLoaded and
 * wires the toggle button to toggleTheme().
 */
(function () {
    const STORAGE_KEY = 'trackph-theme';
    const DARK = 'dark';
    const LIGHT = 'light';

    function getStored() {
        try { return localStorage.getItem(STORAGE_KEY); } catch (_) { return null; }
    }

    function setStored(theme) {
        try { localStorage.setItem(STORAGE_KEY, theme); } catch (_) {}
    }

    function apply(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        const btn = document.getElementById('theme-toggle-btn');
        if (btn) btn.textContent = theme === DARK ? '☀ Light' : '☾ Dark';
    }

    window.initTheme = function () {
        const stored = getStored();
        const preferred = stored || (window.matchMedia('(prefers-color-scheme: dark)').matches ? DARK : LIGHT);
        apply(preferred);
    };

    window.toggleTheme = function () {
        const current = document.documentElement.getAttribute('data-theme') || LIGHT;
        const next = current === DARK ? LIGHT : DARK;
        apply(next);
        setStored(next);
    };
})();
