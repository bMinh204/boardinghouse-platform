const THEME_STORAGE_KEY = "trototn-theme";

document.addEventListener("DOMContentLoaded", initThemeControls);

function initThemeControls() {
    syncThemeToggleLabels();
    document.querySelectorAll("[data-theme-toggle]").forEach(button => {
        button.addEventListener("click", onThemeToggle);
    });
}

function onThemeToggle() {
    const nextTheme = getCurrentTheme() === "dark" ? "light" : "dark";
    applyTheme(nextTheme);
}

function applyTheme(theme) {
    document.documentElement.dataset.theme = theme;
    try {
        localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch (error) {
        // Theme preference is optional, so ignore storage errors.
    }
    syncThemeToggleLabels();
}

function getCurrentTheme() {
    return document.documentElement.dataset.theme === "light" ? "light" : "dark";
}

function syncThemeToggleLabels() {
    const currentTheme = getCurrentTheme();
    const nextLabel = currentTheme === "dark" ? "Giao dien sang" : "Giao dien toi";
    const nextAriaLabel = currentTheme === "dark" ? "Chuyen sang giao dien sang" : "Chuyen sang giao dien toi";

    document.querySelectorAll("[data-theme-toggle]").forEach(button => {
        button.setAttribute("aria-pressed", String(currentTheme === "dark"));
        button.setAttribute("aria-label", nextAriaLabel);

        const label = button.querySelector("[data-theme-label]");
        if (label) {
            label.textContent = nextLabel;
        }
    });
}
