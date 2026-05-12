class ProfileModel {
    constructor() {
        this.state = {
            targetUser: null,
            currentUser: null,
            isOwner: false
        };
    }

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

    async loadProfile(targetId) {
        try {
            const meData = await this.api("/api/auth/me");
            this.state.currentUser = meData.user;
        } catch (err) {
            this.state.currentUser = null;
        }

        // Nếu không truyền ID thì ngầm định xem thông tin của chính mình
        if (!targetId) {
            if (!this.state.currentUser) throw new Error("Vui lòng đăng nhập để xem thông tin.");
            this.state.targetUser = this.state.currentUser;
            this.state.isOwner = true;
            return;
        }

        // Nếu có truyền ID, lấy thông tin user đó
        try {
            const targetData = await this.api(`/api/users/${targetId}`);
            this.state.targetUser = targetData.user || targetData;
        } catch (err) {
            if (this.state.currentUser && String(this.state.currentUser.id) === String(targetId)) {
                this.state.targetUser = this.state.currentUser;
            } else {
                throw new Error("Không thể tải thông tin người dùng này.");
            }
        }

        this.state.isOwner = !!(this.state.currentUser && String(this.state.currentUser.id) === String(this.state.targetUser.id));
    }

    async updateProfile(payload) {
        if (!this.state.isOwner) throw new Error("Bạn không có quyền sửa thông tin này.");
        
        const data = await this.api(`/api/users/${this.state.targetUser.id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });

        const updatedUser = data.user || { ...this.state.targetUser, ...payload };
        this.state.targetUser = updatedUser;
        this.state.currentUser = updatedUser;
    }

    async changePassword(payload) {
        if (!this.state.isOwner) throw new Error("Bạn không có quyền thực hiện thao tác này.");
        await this.api(`/api/auth/change-password`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
    }
}

class ProfileView {
    constructor() {
        this.refs = {
            profileForm: document.getElementById("profileForm"),
            saveBtn: document.getElementById("saveBtn"),
            editBtn: document.getElementById("editBtn"),
            cancelBtn: document.getElementById("cancelBtn"),
            pageTitle: document.getElementById("pageTitle"),
            passwordSection: document.getElementById("passwordSection"),
            passwordForm: document.getElementById("passwordForm"),
            toast: document.getElementById("toast")
        };
        this.toastTimer = null;
    }

    bindEvents(controller) {
        this.refs.profileForm.addEventListener("submit", e => controller.onUpdate(e));
        if (this.refs.editBtn) this.refs.editBtn.addEventListener("click", () => controller.onEditClick());
        if (this.refs.cancelBtn) this.refs.cancelBtn.addEventListener("click", () => controller.onCancelClick());
        if (this.refs.passwordForm) this.refs.passwordForm.addEventListener("submit", e => controller.onChangePassword(e));
    }

    renderProfile(state) {
        const user = state.targetUser;
        const form = this.refs.profileForm;
        const isOwner = state.isOwner;

        this.refs.pageTitle.textContent = isOwner ? "Hồ sơ của bạn" : `Hồ sơ của ${user.fullName}`;

        form.fullName.value = user.fullName || "";
        form.email.value = user.email || "";
        form.phone.value = user.phone || "";
        form.address.value = user.address || "";
        
        const roles = { TENANT: "Người thuê", LANDLORD: "Chủ trọ", ADMIN: "Admin" };
        form.role.value = roles[user.role] || user.role || "";

        this.setEditMode(false, isOwner);
    }

    setEditMode(isEditing, isOwner) {
        const form = this.refs.profileForm;
        if (this.refs.passwordSection) {
            this.refs.passwordSection.classList.toggle("hidden", !isOwner);
        }

        if (isOwner) {
            form.fullName.disabled = !isEditing;
            form.phone.disabled = !isEditing;
            form.address.disabled = !isEditing;
            
            this.refs.saveBtn.classList.toggle("hidden", !isEditing);
            this.refs.cancelBtn.classList.toggle("hidden", !isEditing);
            this.refs.editBtn.classList.toggle("hidden", isEditing);
        } else {
            if (this.refs.editBtn) this.refs.editBtn.classList.add("hidden");
        }
    }

    showToast(message, isError = false) {
        this.refs.toast.textContent = message;
        this.refs.toast.classList.remove("hidden");
        this.refs.toast.style.background = isError ? "#cfcfcf" : "#ffffff";
        this.refs.toast.style.color = "#111111";
        this.refs.toast.style.border = "1px solid #111111";
        clearTimeout(this.toastTimer);
        this.toastTimer = setTimeout(() => this.refs.toast.classList.add("hidden"), 3200);
    }
}

class ProfileController {
    constructor(model, view) {
        this.model = model;
        this.view = view;
        this.init();
    }

    async init() {
        this.view.bindEvents(this);
        const params = new URLSearchParams(window.location.search);
        const targetId = params.get("id");

        try {
            await this.model.loadProfile(targetId);
            this.view.renderProfile(this.model.state);
        } catch (error) {
            this.view.showToast(error.message, true);
            setTimeout(() => window.location.href = "/auth.html", 1500);
        }
    }

    onEditClick() {
        if (this.model.state.isOwner) this.view.setEditMode(true, true);
    }

    onCancelClick() {
        this.view.renderProfile(this.model.state);
    }

    async onUpdate(event) {
        event.preventDefault();
        const formData = new FormData(event.target);
        const payload = {
            fullName: formData.get("fullName"),
            phone: formData.get("phone"),
            address: formData.get("address")
        };

        try {
            await this.model.updateProfile(payload);
            this.view.showToast("Cập nhật thông tin thành công!");
            this.view.renderProfile(this.model.state);
        } catch (error) {
            this.view.showToast(error.message || "Cập nhật thất bại. Xin kiểm tra lại kết nối.", true);
        }
    }

    async onChangePassword(event) {
        event.preventDefault();
        const formData = new FormData(event.target);
        const payload = Object.fromEntries(formData.entries());

        if (payload.newPassword !== payload.confirmPassword) {
            return this.view.showToast("Mật khẩu xác nhận không khớp.", true);
        }
        if (payload.newPassword.length < 6) {
            return this.view.showToast("Mật khẩu mới tối thiểu 6 ký tự.", true);
        }

        try {
            await this.model.changePassword(payload);
            this.view.showToast("Đổi mật khẩu thành công!");
            event.target.reset();
        } catch (error) {
            this.view.showToast(error.message || "Đổi mật khẩu thất bại.", true);
        }
    }
}

document.addEventListener("DOMContentLoaded", () => {
    new ProfileController(new ProfileModel(), new ProfileView());
});
