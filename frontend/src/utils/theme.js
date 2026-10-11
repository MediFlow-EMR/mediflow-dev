const STORAGE_KEY = 'mediflow-theme';

export function readTheme() {
    try {
        return localStorage.getItem(STORAGE_KEY) === 'dark' ? 'dark' : 'light';
    } catch {
        return 'light';
    }
}

export function applyTheme(theme) {
    const resolved = theme === 'dark' ? 'dark' : 'light';
    document.documentElement.dataset.theme = resolved;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', resolved === 'dark' ? '#131b26' : '#0ea5e9');
    return resolved;
}

export function saveTheme(theme) {
    applyTheme(theme);
    try {
        localStorage.setItem(STORAGE_KEY, theme);
    } catch {
        // Theme switching still works when browser storage is unavailable.
    }
    window.dispatchEvent(new Event('mediflow-theme-change'));
}
