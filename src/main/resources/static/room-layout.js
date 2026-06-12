class RoomLayoutPage {
    constructor() {
        this.roomId = new URLSearchParams(window.location.search).get("id");
        this.layout = null;
        this.user = null;
        this.canManage = false;
        this.toast = document.getElementById("toast");
        this.countdownTimer = null;
        this.init();
    }

    async init() {
        this.bindEvents();
        if (!this.roomId || !/^\d+$/.test(this.roomId)) {
            this.renderError("Đường dẫn nhà trọ không hợp lệ.");
            return;
        }
        document.getElementById("detailLink").href = `/room-detail.html?id=${this.roomId}`;
        await this.loadUser();
        this.renderUser();
        await this.loadLayout();
    }

    bindEvents() {
        document.addEventListener("submit", event => this.onSubmit(event));
        document.addEventListener("click", event => this.onClick(event));
        document.addEventListener("change", event => this.onChange(event));
        document.getElementById("logoutBtn").addEventListener("click", () => this.logout());
    }

    async api(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            headers: { "Content-Type": "application/json", ...(options.headers || {}) },
            ...options
        });
        let data = {};
        try {
            data = await response.json();
        } catch (error) {
            data = {};
        }
        if (!response.ok) {
            throw new Error(data.message || data.error || "Yêu cầu thất bại.");
        }
        return data;
    }

    async loadUser() {
        try {
            const data = await this.api("/api/auth/me");
            this.user = data.user || null;
        } catch (error) {
            this.user = null;
        }
    }

    async loadLayout() {
        try {
            const data = await this.api(`/api/rooms/${this.roomId}/layout`);
            this.layout = data.layout;
            this.canManage = Boolean(data.canManage);
            this.render();
        } catch (error) {
            this.renderError(error.message);
        }
    }

    renderUser() {
        if (!this.user) return;
        const authLink = document.getElementById("authLink");
        authLink.textContent = this.user.fullName;
        authLink.href = "/profile.html";
        document.getElementById("logoutBtn").classList.remove("hidden");
    }

    render() {
        const layout = this.layout;
        document.title = `Sơ đồ ${layout.propertyName} | Trọ Tốt ICTU`;
        document.getElementById("layoutManager").classList.toggle("hidden", !this.canManage);
        document.getElementById("layoutSummary").innerHTML = `
            <div class="layout-summary">
                <div>
                    <p class="eyebrow">Sơ đồ nhà trọ</p>
                    <h1>${escapeHtml(layout.propertyName)}</h1>
                    <p class="muted-text">Mỗi ô tương ứng với một phòng vật lý thuộc khu hoặc tầng cụ thể.</p>
                </div>
                <div class="layout-counts">
                    <div><strong>${layout.totalRooms}</strong><span>Tổng phòng</span></div>
                    <div><strong>${layout.availableRooms}</strong><span>Còn trống</span></div>
                    <div><strong>${layout.occupiedRooms}</strong><span>Đã thuê</span></div>
                    <div><strong>${layout.heldRooms}</strong><span>Đang giữ</span></div>
                    <div><strong>${layout.expiringSoonRooms}</strong><span>Sắp hết HĐ</span></div>
                    <div><strong>${layout.maintenanceRooms}</strong><span>Bảo trì</span></div>
                </div>
            </div>`;

        document.getElementById("sectionList").innerHTML = layout.sections.length
            ? layout.sections.map(section => this.sectionView(section)).join("")
            : `<div class="empty-state">${this.canManage
                ? "Chưa có khu hoặc tầng. Hãy tạo khu/tầng đầu tiên ở phía trên."
                : "Chủ trọ chưa thiết lập sơ đồ phòng."}</div>`;
        this.startCountdowns();
    }

    sectionView(section) {
        return `
            <article class="layout-section-card">
                <div class="layout-section-head">
                    <div>
                        <p class="eyebrow">Khu / tầng</p>
                        <h3>${escapeHtml(section.name)}</h3>
                        <span class="muted-badge">${section.rooms.length} phòng</span>
                    </div>
                    ${this.canManage ? `
                        <button class="ghost-button danger-button" type="button"
                            data-action="delete-section" data-section-id="${section.id}">Xóa khu/tầng</button>` : ""}
                </div>
                <div class="physical-room-grid">
                    ${section.rooms.length
                        ? section.rooms.map(room => this.roomView(room)).join("")
                        : `<div class="empty-state">Khu/tầng này chưa có phòng.</div>`}
                </div>
                ${this.canManage ? `
                    <form class="add-rooms-form" data-section-id="${section.id}">
                        <label class="field-group">
                            <span class="field-label">Số phòng</span>
                            <input name="roomNumbers" placeholder="101, 102, 103" required>
                        </label>
                        <label class="field-group">
                            <span class="field-label">Trạng thái ban đầu</span>
                            <select name="status">${statusOptions("AVAILABLE")}</select>
                        </label>
                        <button class="primary-button" type="submit">Thêm phòng</button>
                    </form>
                    <p class="form-hint">Có thể nhập nhiều số phòng, ngăn cách bằng dấu phẩy.</p>` : ""}
            </article>`;
    }

    roomView(room) {
        const holdAction = this.holdAction(room);
        return `
            <div class="physical-room status-${room.status.toLowerCase()}">
                <strong>${escapeHtml(room.roomNumber)}</strong>
                <span>${statusLabel(room.status)}</span>
                ${room.status === "HELD" && room.holdExpiresAt
                    ? `<span class="hold-countdown" data-expires-at="${room.holdExpiresAt}">Đang tính thời gian...</span>`
                    : ""}
                ${holdAction}
                ${this.canManage ? `
                    <select class="room-status-select" data-room-id="${room.id}" aria-label="Trạng thái phòng ${escapeHtml(room.roomNumber)}">
                        ${statusOptions(room.status)}
                    </select>
                    <button class="room-delete-button" type="button" data-action="delete-room"
                        data-room-id="${room.id}" aria-label="Xóa phòng ${escapeHtml(room.roomNumber)}">Xóa</button>` : ""}
            </div>`;
    }

    holdAction(room) {
        if (this.canManage) return "";
        if (room.status === "AVAILABLE") {
            if (this.user?.role === "TENANT") {
                return `<button class="room-hold-button" type="button" data-action="open-hold-dialog"
                    data-room-id="${room.id}" data-room-number="${escapeHtml(room.roomNumber)}">Giữ phòng 24 giờ</button>`;
            }
            if (!this.user) {
                const returnUrl = encodeURIComponent(window.location.pathname + window.location.search);
                return `<a class="room-hold-button link-button" href="/auth.html?returnUrl=${returnUrl}">Đăng nhập để giữ</a>`;
            }
        }
        if (room.status === "HELD" && room.heldByCurrentUser) {
            return `<button class="room-hold-button" type="button" data-action="cancel-hold"
                data-room-id="${room.id}">Hủy giữ phòng</button>`;
        }
        return "";
    }

    async onSubmit(event) {
        const form = event.target;
        if (form.id !== "sectionForm" && form.id !== "holdRoomForm" && !form.matches(".add-rooms-form")) return;
        event.preventDefault();
        try {
            if (form.id === "sectionForm") {
                const values = Object.fromEntries(new FormData(form).entries());
                await this.api(`/api/rooms/${this.roomId}/layout/sections`, {
                    method: "POST",
                    body: JSON.stringify({
                        name: values.name,
                        displayOrder: numberOrNull(values.displayOrder)
                    })
                });
                form.reset();
                this.showToast("Đã thêm khu/tầng.");
            } else if (form.matches(".add-rooms-form")) {
                const values = Object.fromEntries(new FormData(form).entries());
                const roomNumbers = [...new Set(values.roomNumbers.split(/[,;\n]+/)
                    .map(value => value.trim()).filter(Boolean))];
                if (!roomNumbers.length) throw new Error("Vui lòng nhập ít nhất một số phòng.");
                for (const roomNumber of roomNumbers) {
                    await this.api(`/api/rooms/${this.roomId}/layout/physical-rooms`, {
                        method: "POST",
                        body: JSON.stringify({
                            sectionId: Number(form.dataset.sectionId),
                            roomNumber,
                            status: values.status
                        })
                    });
                }
                form.reset();
                this.showToast(`Đã thêm ${roomNumbers.length} phòng.`);
            } else {
                const values = Object.fromEntries(new FormData(form).entries());
                await this.api(`/api/interactions/rooms/${this.roomId}/physical-rooms/${values.physicalRoomId}/hold`, {
                    method: "POST",
                    body: JSON.stringify({
                        moveInDate: values.moveInDate,
                        note: values.note
                    })
                });
                document.getElementById("holdRoomDialog").close();
                form.reset();
                this.showToast("Đã giữ phòng trong 24 giờ và gửi yêu cầu cho chủ trọ.");
            }
            await this.loadLayout();
        } catch (error) {
            this.showToast(error.message, true);
        }
    }

    async onChange(event) {
        const select = event.target.closest(".room-status-select");
        if (!select) return;
        try {
            await this.api(`/api/rooms/${this.roomId}/layout/physical-rooms/${select.dataset.roomId}`, {
                method: "PATCH",
                body: JSON.stringify({ status: select.value })
            });
            this.showToast("Đã cập nhật trạng thái phòng.");
            await this.loadLayout();
        } catch (error) {
            this.showToast(error.message, true);
            await this.loadLayout();
        }
    }

    async onClick(event) {
        const button = event.target.closest("[data-action]");
        if (!button) return;
        try {
            if (button.dataset.action === "open-hold-dialog") {
                const dialog = document.getElementById("holdRoomDialog");
                const form = document.getElementById("holdRoomForm");
                form.physicalRoomId.value = button.dataset.roomId;
                form.moveInDate.min = new Date().toISOString().slice(0, 10);
                form.moveInDate.value = form.moveInDate.min;
                document.getElementById("holdRoomTitle").textContent = `Phòng ${button.dataset.roomNumber}`;
                dialog.showModal();
                return;
            }
            if (button.dataset.action === "close-hold-dialog") {
                document.getElementById("holdRoomDialog").close();
                return;
            }
            if (button.dataset.action === "cancel-hold") {
                if (!window.confirm("Hủy giữ phòng và trả phòng về trạng thái còn trống?")) return;
                await this.api(`/api/interactions/rooms/${this.roomId}/physical-rooms/${button.dataset.roomId}/hold`, {
                    method: "DELETE"
                });
                this.showToast("Đã hủy giữ phòng.");
            }
            if (button.dataset.action === "delete-room") {
                if (!window.confirm("Xóa phòng này khỏi sơ đồ?")) return;
                await this.api(`/api/rooms/${this.roomId}/layout/physical-rooms/${button.dataset.roomId}`, {
                    method: "DELETE"
                });
                this.showToast("Đã xóa phòng.");
            }
            if (button.dataset.action === "delete-section") {
                if (!window.confirm("Xóa khu/tầng và toàn bộ phòng bên trong?")) return;
                await this.api(`/api/rooms/${this.roomId}/layout/sections/${button.dataset.sectionId}`, {
                    method: "DELETE"
                });
                this.showToast("Đã xóa khu/tầng.");
            }
            await this.loadLayout();
        } catch (error) {
            this.showToast(error.message, true);
        }
    }

    async logout() {
        try {
            await this.api("/api/auth/logout", { method: "POST" });
        } finally {
            window.location.reload();
        }
    }

    renderError(message) {
        document.getElementById("layoutSummary").innerHTML =
            `<div class="empty-state">${escapeHtml(message)} <a href="/">Quay lại trang chủ</a></div>`;
        document.getElementById("sectionList").innerHTML = "";
    }

    showToast(message, isError = false) {
        this.toast.textContent = message;
        this.toast.style.background = isError ? "#cfcfcf" : "#ffffff";
        this.toast.style.color = "#111111";
        this.toast.classList.remove("hidden");
        clearTimeout(this.toastTimer);
        this.toastTimer = setTimeout(() => this.toast.classList.add("hidden"), 3200);
    }

    startCountdowns() {
        clearInterval(this.countdownTimer);
        const update = () => {
            let shouldReload = false;
            document.querySelectorAll(".hold-countdown").forEach(element => {
                const remaining = new Date(element.dataset.expiresAt).getTime() - Date.now();
                if (remaining <= 0) {
                    element.textContent = "Đang giải phóng phòng...";
                    shouldReload = true;
                    return;
                }
                const hours = Math.floor(remaining / 3_600_000);
                const minutes = Math.floor((remaining % 3_600_000) / 60_000);
                const seconds = Math.floor((remaining % 60_000) / 1_000);
                element.textContent = `Còn ${hours}g ${minutes}p ${seconds}s`;
            });
            if (shouldReload) {
                clearInterval(this.countdownTimer);
                setTimeout(() => this.loadLayout(), 1500);
            }
        };
        update();
        this.countdownTimer = setInterval(update, 1000);
    }
}

function statusOptions(selected) {
    return [
        ["AVAILABLE", "Còn trống"],
        ["HELD", "Đang giữ"],
        ["OCCUPIED", "Đã thuê"],
        ["EXPIRING_SOON", "Sắp hết hợp đồng"],
        ["MAINTENANCE", "Bảo trì"]
    ].map(([value, label]) => `<option value="${value}" ${value === selected ? "selected" : ""}
        ${value === "HELD" ? "disabled" : ""}>${label}</option>`).join("");
}

function statusLabel(status) {
    return {
        AVAILABLE: "Còn trống",
        HELD: "Đang giữ",
        OCCUPIED: "Đã thuê",
        EXPIRING_SOON: "Sắp hết hợp đồng",
        MAINTENANCE: "Bảo trì"
    }[status] || status;
}

function numberOrNull(value) {
    return value === "" || value === null || value === undefined ? null : Number(value);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

document.addEventListener("DOMContentLoaded", () => new RoomLayoutPage());
