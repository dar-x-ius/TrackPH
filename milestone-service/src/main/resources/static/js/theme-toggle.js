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
        const moonIcon = document.getElementById('theme-icon-moon');
        const sunIcon = document.getElementById('theme-icon-sun');
        if (moonIcon && sunIcon) {
            // icon shown is the mode you'd switch TO, not the current one
            moonIcon.style.display = theme === DARK ? 'none' : '';
            sunIcon.style.display = theme === DARK ? '' : 'none';
        }
        if (btn) {
            const label = theme === DARK ? 'Switch to light mode' : 'Switch to dark mode';
            btn.setAttribute('aria-label', label);
            btn.setAttribute('title', label);
        }
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
