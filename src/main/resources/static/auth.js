class AuthModel {
    async api(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                ...(options.headers ?? {})
            },
            ...options
        });
        let data = {};
        try { data = await response.json(); } catch (error) { data = {}; }
        if (!response.ok) throw new Error(data.message || data.error || "Yêu cầu thất bại.");
        return data;
    }

    async checkSession() {
        const data = await this.api("/api/auth/me");
        return data.user;
    }

    async login(payload) {
        return await this.api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    }

    async register(payload) {
        return await this.api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    }

    async forgotPassword(payload) {
        return await this.api("/api/auth/forgot-password", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    }

    async activateAccount(payload) {
        return await this.api("/api/auth/activate-account", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    }

    async resetPassword(payload) {
        return await this.api("/api/auth/reset-password", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    }
}

class AuthView {
    constructor() {
        this.refs = {
            toast: document.getElementById("toast"),
            loginForm: document.getElementById("loginForm"),
            registerForm: document.getElementById("registerForm"),
            forgotPasswordForm: document.getElementById("forgotPasswordForm"),
            activationOtpForm: document.getElementById("activationOtpForm"),
            resetOtpForm: document.getElementById("resetOtpForm"),
            authTabs: document.querySelectorAll(".auth-tab")
        };
        this.toastTimer = null;
    }

    bindEvents(handlers) {
        this.refs.loginForm.addEventListener("submit", handlers.onLogin);
        this.refs.registerForm.addEventListener("submit", handlers.onRegister);
        this.refs.forgotPasswordForm.addEventListener("submit", handlers.onForgotPassword);
        this.refs.activationOtpForm?.addEventListener("submit", handlers.onActivateAccount);
        this.refs.resetOtpForm?.addEventListener("submit", handlers.onResetPassword);
        
        document.querySelector("[data-action='show-forgot-password']")?.addEventListener("click", handlers.onShowForgotPassword);
        document.querySelectorAll("[data-action='back-to-login']").forEach(button => {
            button.addEventListener("click", () => this.switchTab("login"));
        });

        this.refs.authTabs.forEach(tab => {
            tab.addEventListener("click", () => this.switchTab(tab.dataset.tab));
        });
    }

    switchTab(tab) {
        this.refs.authTabs.forEach(item => item.classList.toggle("active", item.dataset.tab === tab));
        this.refs.loginForm.classList.toggle("hidden", tab !== "login");
        this.refs.registerForm.classList.toggle("hidden", tab !== "register");
        this.refs.forgotPasswordForm.classList.add("hidden");
        this.refs.activationOtpForm?.classList.add("hidden");
        this.refs.resetOtpForm?.classList.add("hidden");
    }

    showForgotPassword() {
        this.refs.loginForm.classList.add("hidden");
        this.refs.registerForm.classList.add("hidden");
        this.refs.forgotPasswordForm.classList.remove("hidden");
        this.refs.activationOtpForm?.classList.add("hidden");
        this.refs.resetOtpForm?.classList.add("hidden");
    }

    showActivationOtp(email, otp) {
        this.refs.loginForm.classList.add("hidden");
        this.refs.registerForm.classList.add("hidden");
        this.refs.forgotPasswordForm.classList.add("hidden");
        this.refs.resetOtpForm?.classList.add("hidden");
        this.refs.activationOtpForm?.classList.remove("hidden");
        if (this.refs.activationOtpForm) {
            this.refs.activationOtpForm.email.value = email || "";
            this.refs.activationOtpForm.otp.value = otp || "";
        }
    }

    showResetOtp(email, otp) {
        this.refs.loginForm.classList.add("hidden");
        this.refs.registerForm.classList.add("hidden");
        this.refs.forgotPasswordForm.classList.add("hidden");
        this.refs.activationOtpForm?.classList.add("hidden");
        this.refs.resetOtpForm?.classList.remove("hidden");
        if (this.refs.resetOtpForm) {
            this.refs.resetOtpForm.email.value = email || "";
            this.refs.resetOtpForm.otp.value = otp || "";
        }
    }

    showToast(message, isError = false) {
        const toast = this.refs.toast;
        toast.textContent = message;
        toast.classList.remove("hidden");
        toast.style.background = isError ? "#cfcfcf" : "#ffffff";
        toast.style.color = "#111111";
        toast.style.border = "1px solid #111111";
        clearTimeout(this.toastTimer);
        this.toastTimer = setTimeout(() => toast.classList.add("hidden"), 3200);
    }

    showFormError(form, message) {
        let hint = form.querySelector(".form-error");
        if (!hint) {
            hint = document.createElement("div");
            hint.className = "form-error";
            form.prepend(hint);
        }
        hint.textContent = message;
        hint.style.color = "#6a6a6a";
        hint.style.fontWeight = "600";
        hint.style.marginBottom = "8px";
    }

    showFormMessage(form, message, link) {
        this.showFormError(form, message);
        const hint = form.querySelector(".form-error");
        if (hint && link) {
            const anchor = document.createElement("a");
            anchor.href = link;
            anchor.textContent = "Mo link kiem thu";
            anchor.style.display = "block";
            anchor.style.marginTop = "8px";
            anchor.style.textDecoration = "underline";
            hint.appendChild(anchor);
        }
    }

    toggleSubmitting(form, submitting) {
        const button = form.querySelector("button[type='submit']");
        if (!button) return;
        button.disabled = submitting;
        button.style.opacity = submitting ? "0.7" : "1";
    }
}

class AuthController {
    constructor(model, view) {
        this.model = model;
        this.view = view;
        this.init();
    }

    async init() {
        this.view.bindEvents({
            onLogin: this.handleLogin.bind(this),
            onRegister: this.handleRegister.bind(this),
            onForgotPassword: this.handleForgotPassword.bind(this),
            onActivateAccount: this.handleActivateAccount.bind(this),
            onResetPassword: this.handleResetPassword.bind(this),
            onShowForgotPassword: () => this.view.showForgotPassword()
        });

        try {
            const user = await this.model.checkSession();
            if (user) {
                window.location.href = "/index.html";
            }
        } catch (error) {
            // Ignore session error for guest
        }
    }

    async handleLogin(event) {
        event.preventDefault();
        const form = event.target;
        const payload = Object.fromEntries(new FormData(form).entries());

        if (!payload.email || !payload.password) {
            return this.view.showFormError(form, "Vui lòng nhập email và mật khẩu.");
        }

        this.view.toggleSubmitting(form, true);
        try {
            const data = await this.model.login(payload);
            const role = data?.user?.role ?? "";
            this.view.showToast("Đăng nhập thành công.");
            setTimeout(() => {
                const anchor = role ? `#${role.toLowerCase()}` : "";
                window.location.href = `/index.html${anchor}`;
            }, 500);
        } catch (error) {
            this.view.showFormError(form, error.message || "Đăng nhập thất bại.");
        } finally {
            this.view.toggleSubmitting(form, false);
        }
    }

    async handleRegister(event) {
        event.preventDefault();
        const form = event.target;
        const payload = Object.fromEntries(new FormData(form).entries());

        if (!payload.fullName || !payload.email || !payload.password) {
            return this.view.showFormError(form, "Vui lòng nhập họ tên, email và mật khẩu.");
        }
        if ((payload.password || "").length < 6) {
            return this.view.showFormError(form, "Mật khẩu tối thiểu 6 ký tự.");
        }

        this.view.toggleSubmitting(form, true);
        try {
            const data = await this.model.register(payload);
            this.view.showFormMessage(form, data.message || "Dang ky thanh cong.", data.activationLink);
            this.view.showToast("Dang ky thanh cong.");
            this.view.showActivationOtp(data.email || payload.email, data.devOtp || "");
            form.reset();
        } catch (error) {
            this.view.showFormError(form, error.message || "Đăng ký thất bại.");
        } finally {
            this.view.toggleSubmitting(form, false);
        }
    }

    async handleForgotPassword(event) {
        event.preventDefault();
        const form = event.target;
        const payload = Object.fromEntries(new FormData(form).entries());

        if (!payload.email) {
            return this.view.showFormError(form, "Vui lòng nhập email.");
        }

        this.view.toggleSubmitting(form, true);
        try {
            const data = await this.model.forgotPassword(payload);
            this.view.showFormMessage(form, data.message || "Yeu cau da duoc gui thanh cong.", data.resetLink);
            this.view.showToast(data.message || "Yeu cau da duoc gui thanh cong.");
            this.view.showResetOtp(data.email || payload.email, data.devOtp || "");
            form.reset();
        } catch (error) {
            this.view.showFormError(form, error.message || "Gửi yêu cầu thất bại.");
        } finally {
            this.view.toggleSubmitting(form, false);
        }
    }

    async handleActivateAccount(event) {
        event.preventDefault();
        const form = event.target;
        const payload = Object.fromEntries(new FormData(form).entries());
        if (!payload.email || !payload.otp) {
            return this.view.showFormError(form, "Vui long nhap email va ma OTP.");
        }
        this.view.toggleSubmitting(form, true);
        try {
            const data = await this.model.activateAccount(payload);
            this.view.showToast(data.message || "Kich hoat tai khoan thanh cong.");
            form.reset();
            this.view.switchTab("login");
        } catch (error) {
            this.view.showFormError(form, error.message || "Kich hoat that bai.");
        } finally {
            this.view.toggleSubmitting(form, false);
        }
    }

    async handleResetPassword(event) {
        event.preventDefault();
        const form = event.target;
        const payload = Object.fromEntries(new FormData(form).entries());
        if (!payload.email || !payload.otp || !payload.newPassword) {
            return this.view.showFormError(form, "Vui long nhap day du email, OTP va mat khau moi.");
        }
        if (payload.newPassword !== payload.confirmPassword) {
            return this.view.showFormError(form, "Mat khau xac nhan khong khop.");
        }
        this.view.toggleSubmitting(form, true);
        try {
            const data = await this.model.resetPassword(payload);
            this.view.showToast(data.message || "Doi mat khau thanh cong.");
            form.reset();
            this.view.switchTab("login");
        } catch (error) {
            this.view.showFormError(form, error.message || "Doi mat khau that bai.");
        } finally {
            this.view.toggleSubmitting(form, false);
        }
    }
}

document.addEventListener("DOMContentLoaded", () => {
    new AuthController(new AuthModel(), new AuthView());
});
