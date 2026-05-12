/**
 * ==========================================
 * MODEL: Chịu trách nhiệm quản lý dữ liệu (state) và giao tiếp với API
 * ==========================================
 */
class BrowseModel {
    constructor() {
        this.state = {
            user: null,
            rooms: [],
            favorites: [],
            // ... các state khác từ file cũ
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

    async loadCurrentUser() {
        try {
            const data = await this.api("/api/auth/me");
            this.state.user = data.user;
        } catch (error) {
            this.state.user = null;
        }
    }

    async loadRooms(params = {}) {
        const query = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== null && value !== undefined && String(value).trim() !== "") {
                query.append(key, value);
            }
        });
        const suffix = query.toString() ? `?${query.toString()}` : "";
        const data = await this.api(`/api/rooms${suffix}`);
        this.state.rooms = data.rooms ?? [];
    }

    // ... Thêm các hàm fetch dữ liệu khác (loadFavorites, loadMyRooms...)
}

/**
 * ==========================================
 * VIEW: Chịu trách nhiệm hiển thị giao diện và bắt sự kiện DOM
 * ==========================================
 */
class BrowseView {
    constructor() {
        this.refs = {
            searchForm: document.getElementById("searchForm"),
            roomList: document.getElementById("roomList"),
            resultCount: document.getElementById("resultCount"),
            userBadge: document.getElementById("userBadge"),
            authLink: document.getElementById("authLink"),
            logoutBtn: document.getElementById("logoutBtn"),
            // ... các refs khác
        };
    }

    // ---- Render Methods ----
    renderUser(user) {
        if (!user) {
            this.refs.userBadge.textContent = "Khách tham quan";
            this.refs.authLink.textContent = "Đăng nhập / Đăng ký";
            this.refs.logoutBtn.classList.add("hidden");
            return;
        }
        this.refs.userBadge.textContent = `${user.fullName} - ${user.role}`;
        this.refs.authLink.textContent = user.fullName;
        this.refs.logoutBtn.classList.remove("hidden");
    }

    renderRooms(rooms) {
        this.refs.resultCount.textContent = `${rooms.length} kết quả`;
        if (!rooms.length) {
            this.refs.roomList.innerHTML = `<div class="empty-state">Không tìm thấy phòng phù hợp.</div>`;
            return;
        }
        this.refs.roomList.innerHTML = rooms.map(room => this.roomCardTemplate(room)).join("");
    }

    roomCardTemplate(room) {
        return `
            <article class="room-card">
                <img src="${room.featuredImage}" alt="${room.title}">
                <div class="room-body">
                    <strong>${room.title}</strong>
                    <span class="price-chip">${room.price} VND</span>
                </div>
            </article>
        `;
    }

    // ---- Event Binding Methods ----
    bindSearch(handler) {
        this.refs.searchForm.addEventListener("submit", event => {
            event.preventDefault();
            const formData = new FormData(event.target);
            const params = Object.fromEntries(formData.entries());
            handler(params);
        });
    }

    bindLogout(handler) {
        this.refs.logoutBtn.addEventListener("click", handler);
    }
}

/**
 * ==========================================
 * CONTROLLER: Cầu nối điều phối giữa Model và View
 * ==========================================
 */
class BrowseController {
    constructor(model, view) {
        this.model = model;
        this.view = view;

        // Bind các events từ View sang logic của Controller
        this.view.bindSearch(this.handleSearch.bind(this));
        // this.view.bindLogout(this.handleLogout.bind(this));
        
        // Khởi tạo app
        this.init();
    }

    async init() {
        await this.model.loadCurrentUser();
        await this.model.loadRooms();
        
        this.view.renderUser(this.model.state.user);
        this.view.renderRooms(this.model.state.rooms);
    }

    async handleSearch(params) {
        await this.model.loadRooms(params);
        this.view.renderRooms(this.model.state.rooms);
    }
}

// Khởi chạy ứng dụng khi DOM tải xong
document.addEventListener("DOMContentLoaded", () => {
    const app = new BrowseController(new BrowseModel(), new BrowseView());
});