class BrowseModel {
    constructor() {
        this.state = this.getInitialState();
    }

    getInitialState() {
        return {
            user: null, rooms: [], selectedRoom: null, favorites: [],
            myRooms: [], conversations: [], activeConversation: null,
            rentalRequests: [], landlordDashboard: null, adminDashboard: null,
            pendingRooms: [], editingRoomId: null, chatbot: null, users: [], selectedUser: null,
            userListCollapsed: true,
            adminUserDetailCollapsed: false,
            collapsedUserCards: new Set(),
            scrolledToRole: false, currentPage: 0, totalPages: 1, eventSource: null
        };
    }

    async api(url, options = {}) {
        const headers = { "Content-Type": "application/json", ...(options.headers ?? {}) };
        // Nếu gửi FormData (multipart/form-data), ta phải xóa Content-Type để trình duyệt tự động chèn boundary
        if (options.body instanceof FormData) {
            delete headers["Content-Type"];
        }
        const response = await fetch(url, {
            credentials: "same-origin",
            ...options,
            headers
        });
        let data = {};
        try { data = await response.json(); } catch (error) { data = {}; }
        if (!response.ok) throw new Error(data.message || data.error || "Yêu cầu thất bại.");
        return data;
    }

    async loadRooms(params = {}) {
        const query = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== null && value !== undefined && String(value).trim() !== "") {
                query.append(key, value);
            }
        });
        if (params.page !== undefined) {
            query.set("page", params.page);
        } else {
            query.set("page", this.state.currentPage || 0);
        }
        const suffix = query.toString() ? `?${query.toString()}` : "";
        const data = await this.api(`/api/rooms${suffix}`);
        this.state.rooms = data.rooms ?? [];
        this.state.currentPage = data.currentPage ?? 0;
        this.state.totalPages = data.totalPages ?? 1;
    }
}

class BrowseView {
    constructor() {
        this.refs = {
            authLink: document.getElementById("authLink"),
            authHeroLink: document.getElementById("authHeroLink"),
            userBadge: document.getElementById("userBadge"),
            logoutBtn: document.getElementById("logoutBtn"),
            roomList: document.getElementById("roomList"),
            pagination: document.getElementById("pagination"),
            resultCount: document.getElementById("resultCount"),
            detailContent: document.getElementById("detailContent"),
            tenantSection: document.getElementById("tenantSection"),
            landlordSection: document.getElementById("landlordSection"),
            adminSection: document.getElementById("adminSection"),
            favoriteList: document.getElementById("favoriteList"),
            tenantRentalList: document.getElementById("tenantRentalList"),
            myRoomList: document.getElementById("myRoomList"),
            landlordConversationList: document.getElementById("landlordConversationList"),
            landlordRentalList: document.getElementById("landlordRentalList"),
            landlordStats: document.getElementById("landlordStats"),
            pendingRoomList: document.getElementById("pendingRoomList"),
            adminRentalList: document.getElementById("adminRentalList"),
            adminStats: document.getElementById("adminStats"),
            monthlyReport: document.getElementById("monthlyReport"),
            quarterYearReport: document.getElementById("quarterYearReport"),
            userList: document.getElementById("userList"),
            adminUserDetail: document.getElementById("adminUserDetail"),
            conversationPanel: document.getElementById("conversationPanel"),
            conversationList: document.getElementById("conversationList"),
            messageThread: document.getElementById("messageThread"),
            messageForm: document.getElementById("messageForm"),
            messageInput: document.getElementById("messageInput"),
            chatbotForm: document.getElementById("chatbotForm"),
            chatbotClearBtn: document.getElementById("chatbotClearBtn"),
            chatbotReply: document.getElementById("chatbotReply"),
            roomSubmitBtn: document.getElementById("roomSubmitBtn"),
            roomResetBtn: document.getElementById("roomResetBtn"),
            roomForm: document.getElementById("roomForm"),
            toast: document.getElementById("toast"),
            statRooms: document.getElementById("statRooms"),
            statPending: document.getElementById("statPending")
        };
        this.toastTimer = null;
    }

    bindEvents(controller) {
        document.getElementById("searchForm").addEventListener("submit", e => controller.onSearch(e));
        this.refs.logoutBtn.addEventListener("click", e => controller.onLogout(e));
        this.refs.roomForm.addEventListener("submit", e => controller.onRoomSubmit(e));
        this.refs.roomResetBtn.addEventListener("click", e => controller.resetRoomForm());
        this.refs.messageForm.addEventListener("submit", e => controller.onSendMessage(e));
        this.refs.chatbotForm.addEventListener("submit", e => controller.onAskChatbot(e));
        this.refs.chatbotClearBtn.addEventListener("click", e => controller.onClearChatbot(e));
        document.getElementById("backupBtn").addEventListener("click", e => controller.onBackup(e));
        document.addEventListener("click", e => controller.onDocumentClick(e));
        document.addEventListener("submit", e => controller.onDynamicSubmit(e));
        [this.refs.authLink, this.refs.authHeroLink].forEach(button => {
            if (button) {
                const navigate = event => {
                    event.preventDefault();
                    const action = button.closest("form")?.getAttribute("action") || "/auth.html";
                    window.location.href = new URL(action, window.location.origin).href;
                };
                button.addEventListener("pointerdown", navigate);
                button.addEventListener("mousedown", navigate);
                button.addEventListener("click", navigate);
            }
        });
        if (this.refs.pagination) {
            this.refs.pagination.addEventListener("click", e => {
                const btn = e.target.closest("[data-page]");
                if (btn) {
                    controller.onPageChange(Number(btn.dataset.page));
                }
            });
        }
    }

    renderAll(state) {
        this.renderUser(state);
        this.renderQuickStats(state);
        this.renderRooms(state);
        this.renderTenantSection(state);
        this.renderLandlordSection(state);
        this.renderAdminSection(state);
        this.renderConversationPanel(state);
        this.renderChatbot(state);
        this.renderDetail(state);
    }

    renderUser(state) {
        if (!state.user) {
            this.refs.userBadge.textContent = "Khách tham quan";
            this.refs.authLink.textContent = "Đăng nhập / Đăng ký";
            if (this.refs.authHeroLink) this.refs.authHeroLink.textContent = this.refs.authLink.textContent;
            this.setAuthNavigation("/auth.html");
            this.refs.logoutBtn.classList.add("hidden");
            this.refs.adminUserDetail.classList.toggle("hidden", Boolean(state.adminUserDetailCollapsed));
            return;
        }
        this.refs.userBadge.textContent = `${state.user.fullName} - ${labelRole(state.user.role)}`;
        this.refs.authLink.textContent = state.user.fullName;
        if (this.refs.authHeroLink) this.refs.authHeroLink.textContent = "Xem hồ sơ";
        this.setAuthNavigation("/profile.html");
        this.refs.logoutBtn.classList.remove("hidden");
    }

    setAuthNavigation(action) {
        [this.refs.authLink, this.refs.authHeroLink].forEach(button => {
            const form = button?.closest("form");
            if (form) form.action = action;
        });
    }

    renderQuickStats(state) {
        this.refs.statRooms.textContent = state.rooms.length;
        this.refs.statPending.textContent = state.adminDashboard?.pendingRooms ?? state.pendingRooms.length ?? 0;
    }

    renderRooms(state) {
        this.refs.resultCount.textContent = `${state.rooms.length} kết quả (Trang ${state.currentPage + 1}/${state.totalPages || 1})`;
        if (!state.rooms.length) {
            this.refs.roomList.innerHTML = `<div class="empty-state">Không tìm thấy phòng phù hợp. Thử mở rộng bộ lọc hoặc thay đổi khu vực.</div>`;
            if (this.refs.pagination) this.refs.pagination.classList.add("hidden");
            return;
        }
        this.refs.roomList.innerHTML = state.rooms.map(r => this.roomCard(r, state)).join("");
        this.renderPagination(state);
    }

    renderPagination(state) {
        if (!this.refs.pagination) return;
        if (state.totalPages <= 1) {
            this.refs.pagination.classList.add("hidden");
            return;
        }
        this.refs.pagination.classList.remove("hidden");
        let html = "";
        for (let i = 0; i < state.totalPages; i++) {
            html += `<button class="${i === state.currentPage ? 'primary-button' : 'ghost-button'}" type="button" data-page="${i}">${i + 1}</button>`;
        }
        this.refs.pagination.innerHTML = html;
    }

    renderTenantSection(state) {
        const enabled = state.user?.role === "TENANT";
        this.refs.tenantSection.classList.toggle("hidden", !enabled);
        if (!enabled) return;
        this.refs.favoriteList.innerHTML = state.favorites.length
            ? state.favorites.map(r => this.favoriteRoomItem(r)).join("")
            : `<div class="empty-state">Chưa có phòng yêu thích.</div>`;
        this.refs.tenantRentalList.innerHTML = state.rentalRequests.length
            ? state.rentalRequests.map(r => this.rentalItem(r, state)).join("")
            : `<div class="empty-state">Bạn chưa gửi yêu cầu thuê nào.</div>`;
    }

    renderLandlordSection(state) {
        const enabled = state.user?.role === "LANDLORD";
        this.refs.landlordSection.classList.toggle("hidden", !enabled);
        if (!enabled) return;
        this.refs.landlordStats.innerHTML = state.landlordDashboard ? this.statTiles([
            ["Tổng phòng", state.landlordDashboard.totalRooms],
            ["Còn trống", state.landlordDashboard.availableRooms],
            ["Đã thuê", state.landlordDashboard.occupiedRooms],
            ["Đang sửa", state.landlordDashboard.maintenanceRooms],
            ["Sắp hết HĐ", state.landlordDashboard.expiringSoonRooms],
            ["Lượt xem", state.landlordDashboard.totalViews],
            ["Liên hệ", state.landlordDashboard.totalContacts],
            ["Chờ duyệt", state.landlordDashboard.pendingModeration]
        ]) : "";
        this.refs.myRoomList.innerHTML = state.myRooms.length
            ? state.myRooms.map(r => this.landlordRoomCard(r)).join("")
            : `<div class="empty-state">Bạn chưa có tin đăng nào.</div>`;
        this.refs.landlordConversationList.innerHTML = state.conversations.length
            ? state.conversations.map(c => this.conversationItem(c, state)).join("")
            : `<div class="empty-state">Chưa có hội thoại nào.</div>`;
        this.refs.landlordRentalList.innerHTML = state.rentalRequests.length
            ? state.rentalRequests.map(r => this.rentalItem(r, state)).join("")
            : `<div class="empty-state">Chưa có yêu cầu thuê nào.</div>`;
    }

    renderAdminSection(state) {
        const enabled = state.user?.role === "ADMIN";
        this.refs.adminSection.classList.toggle("hidden", !enabled);
        if (!enabled) return;
        this.refs.adminStats.innerHTML = state.adminDashboard ? this.statTiles([
            ["Tổng user", state.adminDashboard.totalUsers],
            ["Người thuê", state.adminDashboard.tenantCount],
            ["Chủ trọ", state.adminDashboard.landlordCount],
            ["Phòng duyệt", state.adminDashboard.approvedRooms],
            ["Chờ duyệt", state.adminDashboard.pendingRooms],
            ["Đã thuê", state.adminDashboard.occupiedRooms],
            ["Lượt xem", state.adminDashboard.totalViews],
            ["Hội thoại", state.adminDashboard.totalConversations]
        ]) : "";
        this.refs.pendingRoomList.innerHTML = state.pendingRooms.length
            ? state.pendingRooms.map(r => this.adminRoomItem(r)).join("")
            : `<div class="empty-state">Không còn tin đăng chờ duyệt.</div>`;
        this.refs.adminRentalList.innerHTML = state.adminDashboard?.recentRentalRequests?.length
            ? state.adminDashboard.recentRentalRequests.map(r => this.reportRentalItem(r)).join("")
            : `<div class="empty-state">Chưa có dữ liệu yêu cầu thuê.</div>`;
        this.refs.monthlyReport.innerHTML = this.reportList(state.adminDashboard?.monthlyReport ?? []);
        this.refs.quarterYearReport.innerHTML = `${this.reportList(state.adminDashboard?.quarterlyReport ?? [])}${this.reportList(state.adminDashboard?.yearlyReport ?? [])}`;
        this.refs.userList.innerHTML = state.users.length
            ? state.users.map(u => this.userItem(u)).join("")
            : `<div class="empty-state">Chưa có tài khoản nào.</div>`;
        this.renderUserListToggle(state);
        this.renderAdminUserDetail(state);
    }

    renderUserListToggle(state) {
        const list = this.refs.userList;
        if (!list) return;
        const heading = list.previousElementSibling;
        if (heading && heading.dataset.enhanced !== "true") {
            heading.dataset.enhanced = "true";
            heading.classList.add("collapsible-heading");
            const button = document.createElement("button");
            button.id = "userListToggle";
            button.className = "icon-button collapse-button";
            button.type = "button";
            button.dataset.action = "toggle-user-list";
            button.setAttribute("aria-label", "Mở hoặc thu gọn danh sách tài khoản");
            heading.appendChild(button);
        }
        const button = document.getElementById("userListToggle");
        if (button) button.textContent = state.userListCollapsed ? "▸" : "▾";
        list.classList.toggle("hidden", Boolean(state.userListCollapsed));
    }

    renderAdminUserDetail(state) {
        if (!this.refs.adminUserDetail) return;
        this.renderAdminUserDetailToggle(state);
        this.refs.adminUserDetail.classList.toggle("hidden", Boolean(state.adminUserDetailCollapsed));
        const user = state.selectedUser;
        if (!user) {
            this.refs.adminUserDetail.innerHTML = `<div class="empty-state">Bấm vào tên tài khoản trong danh sách để xem thông tin.</div>`;
            return;
        }
        const rows = [
            ["ID", user.id],
            ["Họ và tên", user.fullName],
            ["Email", user.email],
            ["Số điện thoại", user.phone || "Chưa cập nhật"],
            ["Địa chỉ", user.address || "Chưa cập nhật"],
            ["Vai trò", labelRole(user.role)],
            ["Trạng thái", user.locked ? "Đã khóa" : "Hoạt động"],
            ["Kích hoạt", user.active ? "Đã kích hoạt" : "Chưa kích hoạt"],
            ["Ngày tạo", formatDateTime(user.createdAt)]
        ];
        this.refs.adminUserDetail.innerHTML = `
            <div class="stack-item">
                <strong>${escapeHtml(user.fullName)}</strong>
                ${rows.map(([label, value]) => `
                    <p><span class="muted-text">${escapeHtml(label)}:</span> ${escapeHtml(String(value ?? ""))}</p>
                `).join("")}
            </div>`;
    }

    renderAdminUserDetailToggle(state) {
        const detail = this.refs.adminUserDetail;
        if (!detail) return;
        const heading = detail.previousElementSibling;
        if (heading && heading.dataset.enhanced !== "true") {
            heading.dataset.enhanced = "true";
            heading.classList.add("collapsible-heading");
            const button = document.createElement("button");
            button.id = "adminUserDetailToggle";
            button.className = "icon-button collapse-button";
            button.type = "button";
            button.dataset.action = "toggle-admin-user-detail";
            button.setAttribute("aria-label", "Thu gọn chi tiết tài khoản");
            heading.appendChild(button);
        }
        const button = document.getElementById("adminUserDetailToggle");
        if (button) button.textContent = state.adminUserDetailCollapsed ? "▸" : "▾";
    }

    renderConversationPanel(state) {
        const enabled = Boolean(state.user);
        this.refs.conversationPanel.classList.toggle("hidden", !enabled);
        if (!enabled) return;
        this.refs.conversationList.innerHTML = state.conversations.length
            ? state.conversations.map(c => this.conversationItem(c, state)).join("")
            : `<div class="empty-state">Chưa có cuộc trò chuyện nào.</div>`;

        if (!state.activeConversation) {
            this.refs.messageThread.innerHTML = `<div class="empty-state">Chọn cuộc trò chuyện để xem nội dung.</div>`;
            this.refs.messageForm.classList.add("hidden");
            return;
        }
        this.refs.messageThread.innerHTML = state.activeConversation.messages.map(message => `
            <div class="message-bubble ${message.senderId === state.user.id ? "mine" : ""}">
                <strong>${escapeHtml(message.senderName)}</strong>
                <p>${escapeHtml(message.content)}</p>
                <small>${formatDate(message.createdAt)}</small>
            </div>
        `).join("");
        this.refs.messageForm.classList.remove("hidden");
    }

    renderChatbot(state) {
        if (!state.chatbot) {
            this.refs.chatbotReply.innerHTML = `Chatbot sẽ phân tích ngân sách, khu vực và tiện nghi để gợi ý phòng.`;
            this.refs.chatbotReply.classList.add("empty-state");
            return;
        }
        this.refs.chatbotReply.classList.remove("empty-state");
        const suggestions = state.chatbot.suggestions?.length
            ? `<div class="stack-list">${state.chatbot.suggestions.map(r => this.stackRoomItem(r)).join("")}</div>` : "";
        this.refs.chatbotReply.innerHTML = `
            <div class="stack-item">
                <p><strong>Trả lời:</strong> ${escapeHtml(state.chatbot.reply)}</p>
                <div class="stack-meta">
                    ${state.chatbot.budget ? `<span class="muted-badge">Ngân sách: ${formatMoney(state.chatbot.budget)}</span>` : ""}
                    ${state.chatbot.area ? `<span class="muted-badge">Khu vực: ${escapeHtml(state.chatbot.area)}</span>` : ""}
                    ${state.chatbot.amenity ? `<span class="muted-badge">Tiện nghi: ${escapeHtml(state.chatbot.amenity)}</span>` : ""}
                </div>
            </div>
            ${suggestions}
        `;
    }

    renderDetail(state) {
        if (!state.selectedRoom) {
            this.refs.detailContent.innerHTML = `Ảnh, giá, tiện nghi, đánh giá và bản đồ của phòng sẽ hiện tại đây.`;
            return;
        }
        const room = state.selectedRoom;
        const viewCount = Number.isFinite(room.viewCount) ? room.viewCount : 0;
        const contactCount = Number.isFinite(room.contactCount) ? room.contactCount : 0;
        const surveyAverage = room.surveyAverage != null ? room.surveyAverage : "Chưa có";
        const surveyCount = room.surveyCount != null ? room.surveyCount : 0;
        const canRequestRental = ["AVAILABLE", "EXPIRING_SOON"].includes(room.status);
        const rentalRequestPanel = canRequestRental ? `
            <form id="rentalRequestForm" class="stack-form detail-block" data-room-id="${room.id}">
                <h4>Gui yeu cau thue</h4>
                <input name="moveInDate" type="date" required>
                <textarea name="note" placeholder="Mo ta nhu cau, thoi gian vao o, thoi han hop dong..." required></textarea>
                <button class="primary-button" type="submit">Gui yeu cau thue</button>
            </form>
        ` : `
            <div class="detail-block">
                <h4>Gui yeu cau thue</h4>
                <div class="empty-state">Phong dang o trang thai ${escapeHtml(labelRoomStatus(room.status))}, hien khong nhan yeu cau thue moi.</div>
            </div>
        `;
        const tenantActions = state.user?.role === "TENANT" ? `
            <div class="card-actions">
                <button class="primary-button" type="button" data-action="toggle-favorite" data-id="${room.id}">${room.favorite ? "Bỏ yêu thích" : "Lưu yêu thích"}</button>
                <button class="ghost-button" type="button" data-action="open-contact-form" data-id="${room.id}">Nhắn tin chủ trọ</button>
            </div>
            <form id="conversationStartForm" class="stack-form detail-block hidden" data-room-id="${room.id}">
                <textarea name="content" placeholder="Nhập nội dung muốn trao đổi với chủ trọ..." required></textarea>
                <button class="primary-button" type="submit">Bắt đầu chat</button>
            </form>
            ${rentalRequestPanel}
            <form id="surveyForm" class="stack-form detail-block" data-room-id="${room.id}">
                <h4>Đánh giá phòng / nhà trọ</h4>
                <div class="split-two">
                    <input name="cleanlinessRating" type="number" min="1" max="5" placeholder="Vệ sinh (1-5)" required>
                    <input name="securityRating" type="number" min="1" max="5" placeholder="An ninh (1-5)" required>
                </div>
                <input name="convenienceRating" type="number" min="1" max="5" placeholder="Tiện nghi (1-5)" required>
                <textarea name="comment" placeholder="Cảm nhận của bạn..." required></textarea>
                <button class="ghost-button" type="submit">Gửi đánh giá</button>
            </form>
        ` : "";

        const mapUrl = buildGoogleMapsUrl(room);
        const addressText = displayRoomAddress(room);

        this.refs.detailContent.innerHTML = `
            <div class="detail-hero"><img src="${escapeHtml(room.featuredImage)}" alt="${escapeHtml(room.title)}"></div>
            <div class="detail-block">
                <div class="price-line">
                    <h3>${escapeHtml(room.title)}</h3>
                    <span class="price-chip">${formatMoney(room.price)}/tháng</span>
                </div>
                <p class="muted-text">${escapeHtml(room.propertyName)} · ${escapeHtml(addressText)}</p>
                <div class="tag-row">
                    <span class="status-chip">${labelRoomStatus(room.status)}</span>
                    <span class="muted-badge">${room.size} m²</span>
                    <span class="muted-badge">${room.capacity} người</span>
                    <span class="muted-badge">${viewCount} lượt xem</span>
                    <span class="muted-badge">${contactCount} liên hệ</span>
                    <span class="muted-badge">Đánh giá ${surveyAverage} (${surveyCount})</span>
                </div>
                <p>${escapeHtml(room.description)}</p>
                <div class="tag-row">${(room.amenities ?? []).map(amenity => `<span class="tag">${escapeHtml(amenity)}</span>`).join("")}</div>
            </div>
            <div class="gallery">${(room.imageUrls ?? []).map(url => `<img src="${escapeHtml(url)}" alt="Ảnh phòng">`).join("")}</div>
            <div class="detail-block">
                <div class="stack-item">
                    <p><strong>Chủ trọ:</strong> ${escapeHtml(room.ownerName)}</p>
                    <p><strong>Liên hệ:</strong> ${escapeHtml(room.contactPhone ?? room.ownerPhone ?? "")}</p>
                    <p><strong>Địa chỉ:</strong> ${escapeHtml(addressText)}</p>
                    <p><strong>Hợp đồng:</strong> ${escapeHtml(room.contractNote ?? "Đang cập nhật")}</p>
                    <p><strong>Có thể vào ở:</strong> ${formatDate(room.availableFrom)}</p>
                    ${mapUrl ? `<a class="ghost-button" href="${escapeHtml(mapUrl)}" target="_blank" rel="noopener">Mở Google Maps</a>` : ""}
                </div>
                <div id="leafletMap" style="height: 400px; border-radius: 8px; margin-top: 16px; background: #eee;"></div>
            </div>
            ${tenantActions}
            <div class="detail-block">
                <h4>Đánh giá gần đây</h4>
                ${(room.surveys ?? []).length ? room.surveys.map(survey => `
                    <div class="stack-item">
                        <strong>${escapeHtml(survey.userName)}</strong>
                        <div class="stack-meta">
                            <span class="muted-badge">Vệ sinh ${survey.cleanlinessRating}/5</span>
                            <span class="muted-badge">An ninh ${survey.securityRating}/5</span>
                            <span class="muted-badge">Tiện nghi ${survey.convenienceRating}/5</span>
                        </div>
                        <p>${escapeHtml(survey.comment)}</p>
                    </div>
                `).join("") : `<div class="empty-state">Chưa có đánh giá nào.</div>`}
            </div>
        `;
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

    scrollTo(elementId) {
        const el = document.getElementById(elementId);
        if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    fillRoomForm(room) {
        this.refs.roomSubmitBtn.textContent = "Cập nhật phòng";
        const form = this.refs.roomForm;
        form.propertyName.value = room.propertyName ?? "";
        form.title.value = room.title ?? "";
        form.address.value = room.address ?? "";
        form.areaName.value = room.areaName ?? "";
        form.price.value = room.price ?? "";
        form.size.value = room.size ?? "";
        form.capacity.value = room.capacity ?? "";
        form.amenities.value = (room.amenities ?? []).join(", ");
        form.featuredImage.value = room.featuredImage ?? "";
        form.imageUrls.value = (room.imageUrls ?? []).join(", ");
        form.description.value = room.description ?? "";
        form.contractNote.value = room.contractNote ?? "";
        form.mapQuery.value = room.mapQuery ?? "";
        form.contactPhone.value = room.contactPhone ?? "";
        form.status.value = room.status ?? "AVAILABLE";
        form.availableFrom.value = room.availableFrom ?? "";
        this.scrollTo("roomForm");
    }

    resetRoomForm() {
        this.refs.roomSubmitBtn.textContent = "Đăng tin / Cập nhật";
        this.refs.roomForm.reset();
    }

    // ---- Templates ----
    roomCard(room, state) {
        return `
            <article class="room-card">
                <img src="${escapeHtml(room.featuredImage)}" alt="${escapeHtml(room.title)}">
                <div class="room-body">
                    <div class="price-line">
                        <strong>${escapeHtml(room.title)}</strong>
                        <span class="price-chip">${formatMoney(room.price)}</span>
                    </div>
                    <p class="muted-text">${escapeHtml(room.propertyName)} · ${escapeHtml(room.areaName)}</p>
                    <div class="tag-row">
                        <span class="status-chip">${labelRoomStatus(room.status)}</span>
                        <span class="muted-badge">${room.size} m²</span>
                        <span class="muted-badge">${room.capacity} người</span>
                    </div>
                    <div class="tag-row">${(room.amenities ?? []).slice(0, 4).map(a => `<span class="tag">${escapeHtml(a)}</span>`).join("")}</div>
                    <div class="stack-meta">
                        <span class="muted-badge">${room.viewCount ?? 0} xem</span>
                        <span class="muted-badge">${room.contactCount ?? 0} liên hệ</span>
                        <span class="muted-badge">ĐG ${room.surveyAverage ?? "Chưa có"} (${room.surveyCount ?? 0})</span>
                    </div>
                    <div class="card-actions">
                        <button class="primary-button" type="button" data-action="view-room" data-id="${room.id}">Xem chi tiết</button>
                        ${state.user?.role === "TENANT" ? `<button class="ghost-button" type="button" data-action="toggle-favorite" data-id="${room.id}">${room.favorite ? "Bỏ lưu" : "Yêu thích"}</button>` : ""}
                        ${state.user?.role === "ADMIN" ? `<button class="ghost-button danger-button" type="button" data-action="delete-room" data-id="${room.id}">Xóa</button>` : ""}
                    </div>
                </div>
            </article>`;
    }
    landlordRoomCard(room) {
        return `
            <article class="room-card">
                <img src="${escapeHtml(room.featuredImage)}" alt="${escapeHtml(room.title)}">
                <div class="room-body">
                    <div class="price-line">
                        <strong>${escapeHtml(room.title)}</strong>
                        <span class="price-chip">${formatMoney(room.price)}</span>
                    </div>
                    <p class="muted-text">${escapeHtml(displayRoomAddress(room))}</p>
                    <div class="tag-row">
                        <span class="status-chip">${labelRoomStatus(room.status)}</span>
                        <span class="muted-badge">${labelModeration(room.moderationStatus)}</span>
                        <span class="muted-badge">${room.viewCount ?? 0} xem</span>
                    </div>
                    <div class="card-actions">
                        <button class="primary-button" type="button" data-action="view-room" data-id="${room.id}">Chi tiết</button>
                        <button class="ghost-button" type="button" data-action="edit-room" data-id="${room.id}">Chỉnh sửa</button>
                        <button class="ghost-button" type="button" data-action="update-room-status" data-id="${room.id}" data-status="AVAILABLE">Còn trống</button>
                        <button class="ghost-button" type="button" data-action="update-room-status" data-id="${room.id}" data-status="OCCUPIED">Đã thuê</button>
                        <button class="ghost-button" type="button" data-action="delete-room" data-id="${room.id}">Xóa</button>
                    </div>
                </div>
            </article>`;
    }
    adminRoomItem(room) {
        return `
            <div class="stack-item">
                <strong>${escapeHtml(room.title)}</strong>
                <p>${escapeHtml(room.propertyName)} · ${escapeHtml(displayRoomAddress(room))}</p>
                <div class="stack-meta">
                    <span class="muted-badge">${labelRoomStatus(room.status)}</span>
                    <span class="muted-badge">${formatMoney(room.price)}</span>
                </div>
                <div class="stack-actions">
                    <button class="primary-button" type="button" data-action="view-room" data-id="${room.id}">Xem</button>
                    <button class="ghost-button" type="button" data-action="moderate-room" data-id="${room.id}" data-status="APPROVED">Duyệt</button>
                    <button class="ghost-button" type="button" data-action="moderate-room" data-id="${room.id}" data-status="REJECTED">Từ chối</button>
                    <button class="ghost-button danger-button" type="button" data-action="delete-room" data-id="${room.id}">Xóa</button>
                </div>
            </div>`;
    }
    userItem(user) {
        return `
            <div class="stack-item">
                <button class="text-button user-name-button" type="button" data-action="user-detail" data-id="${user.id}">
                    ${escapeHtml(user.fullName)} (${escapeHtml(user.email)})
                </button>
                <div class="stack-meta">
                    <span class="muted-badge">${labelRole(user.role)}</span>
                    ${user.locked ? `<span class="muted-badge">Đã khóa</span>` : `<span class="muted-badge">Hoạt động</span>`}
                </div>
                <div class="stack-actions">
                    <button class="ghost-button" type="button" data-action="user-role" data-id="${user.id}" data-role="TENANT">Thuê</button>
                    <button class="ghost-button" type="button" data-action="user-role" data-id="${user.id}" data-role="LANDLORD">Chủ trọ</button>
                    <button class="ghost-button" type="button" data-action="user-role" data-id="${user.id}" data-role="ADMIN">Admin</button>
                    <button class="primary-button" type="button" data-action="user-status" data-id="${user.id}" data-locked="${!user.locked}">${user.locked ? "Mở khóa" : "Khóa"}</button>
                </div>
            </div>`;
    }
    stackRoomItem(room) {
        return `
            <div class="stack-item">
                <strong>${escapeHtml(room.title)}</strong>
                <p>${escapeHtml(room.propertyName)} · ${escapeHtml(room.areaName)}</p>
                <div class="stack-meta">
                    <span class="muted-badge">${formatMoney(room.price)}</span>
                    <span class="muted-badge">${room.size} m²</span>
                </div>
                <div class="stack-actions">
                    <button class="primary-button" type="button" data-action="view-room" data-id="${room.id}">Xem chi tiết</button>
                </div>
            </div>`;
    }
    favoriteRoomItem(room) {
        return `
            <div class="stack-item">
                <strong>${escapeHtml(room.title)}</strong>
                <p>${escapeHtml(room.propertyName)} · ${escapeHtml(room.areaName)}</p>
                <div class="stack-meta">
                    <span class="muted-badge">${formatMoney(room.price)}</span>
                    <span class="muted-badge">${room.size} m²</span>
                </div>
                <div class="stack-actions">
                    <button class="primary-button" type="button" data-action="view-room" data-id="${room.id}">Xem chi tiết</button>
                    <button class="ghost-button" type="button" data-action="toggle-favorite" data-id="${room.id}">Hủy yêu thích</button>
                </div>
            </div>`;
    }
    rentalItem(item, state) {
        const canApprove = state.user?.role === "LANDLORD" || state.user?.role === "ADMIN";
        const canCancel = state.user?.role === "TENANT" && item.status === "PENDING";
        return `
            <div class="stack-item">
                <strong>${escapeHtml(item.room.title)}</strong>
                <p>Ngày vào ở: ${formatDate(item.moveInDate)}</p>
                <p>${escapeHtml(item.note)}</p>
                <div class="stack-meta">
                    <span class="muted-badge">${escapeHtml(item.tenant.fullName)}</span>
                    <span class="muted-badge">${escapeHtml(item.landlord.fullName)}</span>
                    <span class="muted-badge">${labelRentalStatus(item.status)}</span>
                </div>
                <div class="stack-actions">
                    <button class="primary-button" type="button" data-action="view-room" data-id="${item.room.id}">Xem phòng</button>
                    ${canApprove ? `<button class="ghost-button" type="button" data-action="rental-status" data-id="${item.id}" data-status="APPROVED">Duyệt</button><button class="ghost-button" type="button" data-action="rental-status" data-id="${item.id}" data-status="REJECTED">Từ chối</button>` : ""}
                    ${canCancel ? `<button class="ghost-button" type="button" data-action="rental-status" data-id="${item.id}" data-status="CANCELLED">Hủy yêu cầu</button>` : ""}
                </div>
            </div>`;
    }
    reportRentalItem(item) {
        return `
            <div class="stack-item compact">
                <strong>${escapeHtml(item.roomTitle)}</strong>
                <p>${escapeHtml(item.tenantName)} ↔ ${escapeHtml(item.landlordName)}</p>
                <div class="stack-meta">
                    <span class="muted-badge">${labelRentalStatus(item.status)}</span>
                    <span class="muted-badge">${formatDate(item.updatedAt)}</span>
                </div>
            </div>`;
    }
    conversationItem(item, state) {
        const partner = state.user?.role === "TENANT" ? item.landlord.fullName : item.tenant.fullName;
        return `
            <div class="stack-item compact">
                <strong>${escapeHtml(item.room.title)}</strong>
                <p>${escapeHtml(partner)}</p>
                <div class="stack-meta">
                    <span class="muted-badge">${formatDate(item.updatedAt)}</span>
                </div>
                <div class="stack-actions">
                    <button class="ghost-button" type="button" data-action="select-conversation" data-id="${item.id}">Mở chat</button>
                    <button class="primary-button" type="button" data-action="view-room" data-id="${item.room.id}">Phòng</button>
                </div>
            </div>`;
    }
    reportList(items) {
        if (!items.length) return `<div class="empty-state">Chưa có dữ liệu.</div>`;
        return items.map(item => `
            <div class="stack-item compact">
                <strong>${escapeHtml(item.label)}</strong>
                <p>${item.value} lượt</p>
            </div>`).join("");
    }
    statTiles(items) {
        return items.map(([label, value]) => `
            <div class="stat-tile">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(String(value))}</strong>
            </div>`).join("");
    }
}

class BrowseController {
    constructor(model, view) {
        this.model = model;
        this.view = view;
        this.view.model = model;
        this.mapInstance = null;
        this.init();
    }

    async init() {
        this.view.bindEvents(this);
        await this.refreshApp();
        this.scrollToRoleSectionFromHash();
    }

    async refreshApp() {
        const selectedId = this.model.state.selectedRoom?.id ?? null;
        const currentEventSource = this.model.state.eventSource;
        Object.assign(this.model.state, this.model.getInitialState(), { 
            scrolledToRole: this.model.state.scrolledToRole,
            eventSource: currentEventSource
        });

        try {
            const userData = await this.model.api("/api/auth/me");
            this.model.state.user = userData.user;
        } catch (error) {
            this.model.state.user = null;
        }

        await this.model.loadRooms();

        const tasks = [];
        const state = this.model.state;
        if (state.user) {
            tasks.push(
                this.model.api("/api/interactions/conversations").then(d => state.conversations = d.conversations ?? []),
                this.model.api("/api/interactions/rental-requests").then(d => state.rentalRequests = d.rentalRequests ?? [])
            );
            if (state.user.role === "TENANT") {
                tasks.push(this.model.api("/api/rooms/favorites").then(d => state.favorites = d.rooms ?? []));
            } else if (state.user.role === "LANDLORD") {
                tasks.push(
                    this.model.api("/api/rooms/mine").then(d => state.myRooms = d.rooms ?? []),
                    this.model.api("/api/dashboard/landlord").then(d => state.landlordDashboard = d.dashboard)
                );
            } else if (state.user.role === "ADMIN") {
                tasks.push(
                    this.model.api("/api/rooms/pending").then(d => state.pendingRooms = d.rooms ?? []),
                    this.model.api("/api/dashboard/admin").then(d => state.adminDashboard = d.dashboard),
                    this.model.api("/api/users").then(d => state.users = d.users ?? [])
                );
            }
        }
        await Promise.all(tasks.map(task => task.catch(error => console.error(error))));
        this.setupRealtime();
        this.view.renderAll(state);

        if (selectedId) await this.selectRoom(selectedId, false);
        this.autoScrollToRole();
    }

    async initLeafletMap(room) {
        if (this.mapInstance) {
            this.mapInstance.remove();
            this.mapInstance = null;
        }

        const mapContainer = document.getElementById("leafletMap");
        if (!mapContainer) return;

        const mapUrl = buildGoogleMapsUrl(room);
        const query = buildMapSearchQuery(room);
        const coordinates = extractMapCoordinates(room.address) || extractMapCoordinates(room.mapQuery);

        if (!query && !coordinates && !mapUrl) {
            mapContainer.innerHTML = '<div class="empty-state">Không có thông tin địa chỉ để hiển thị bản đồ.</div>';
            return;
        }

        const fallbackMap = (message) => {
            if (this.mapInstance) {
                this.mapInstance.remove();
                this.mapInstance = null;
            }
            if (!window.L) {
                mapContainer.innerHTML = `<div class="empty-state">${escapeHtml(message)} ${mapUrl ? `<a href="${escapeHtml(mapUrl)}" target="_blank" rel="noopener">Mở trên Google Maps</a>` : ""}</div>`;
                return;
            }

            const defaultLatLng = [21.5942, 105.8482];
            this.mapInstance = L.map(mapContainer).setView(defaultLatLng, 13);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(this.mapInstance);
            L.marker(defaultLatLng).addTo(this.mapInstance)
                .bindPopup(`<b>${escapeHtml(room.title)}</b><br>${escapeHtml(query || displayRoomAddress(room) || room.areaName || "")}<br><small>${escapeHtml(message)}</small>${mapUrl ? `<br><a href="${escapeHtml(mapUrl)}" target="_blank" rel="noopener">Mở Google Maps</a>` : ""}`)
                .openPopup();
        };

        try {
            if (!window.L || !window.GeoSearch?.OpenStreetMapProvider) {
                fallbackMap("Không thể tải dịch vụ tìm địa chỉ, đang hiển thị khu vực gần Thái Nguyên.");
                return;
            }

            if (coordinates) {
                this.mapInstance = L.map(mapContainer).setView([coordinates.lat, coordinates.lng], 16);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                }).addTo(this.mapInstance);
                L.marker([coordinates.lat, coordinates.lng]).addTo(this.mapInstance)
                    .bindPopup(`<b>${escapeHtml(room.title)}</b><br>${escapeHtml(displayRoomAddress(room) || room.mapQuery || "")}${mapUrl ? `<br><a href="${escapeHtml(mapUrl)}" target="_blank" rel="noopener">Mở Google Maps</a>` : ""}`)
                    .openPopup();
                return;
            }

            if (isMapsUrl(room.address) && !query) {
                mapContainer.innerHTML = `<div class="empty-state">Địa chỉ đang là link Google Maps rút gọn nên hệ thống không đọc được tọa độ trực tiếp. <a href="${escapeHtml(mapUrl)}" target="_blank" rel="noopener">Mở Google Maps</a></div>`;
                return;
            }

            const provider = new GeoSearch.OpenStreetMapProvider();
            const results = await provider.search({ query });

            if (results && results.length > 0) {
                const { x: lng, y: lat, label } = results[0];
                
                this.mapInstance = L.map(mapContainer).setView([lat, lng], 16);

                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                }).addTo(this.mapInstance);

                L.marker([lat, lng]).addTo(this.mapInstance)
                    .bindPopup(`<b>${escapeHtml(room.title)}</b><br>${escapeHtml(label)}${mapUrl ? `<br><a href="${escapeHtml(mapUrl)}" target="_blank" rel="noopener">Mở Google Maps</a>` : ""}`)
                    .openPopup();

                const search = new GeoSearch.GeoSearchControl({
                    provider: provider, style: 'bar', showMarker: true, showPopup: false,
                    marker: { icon: new L.Icon.Default(), draggable: false },
                    popupFormat: ({ result }) => result.label, resultFormat: ({ result }) => result.label,
                    maxMarkers: 1, retainZoomLevel: false, animateZoom: true, autoClose: true,
                    searchLabel: 'Tìm kiếm địa chỉ...', keepResult: true,
                });
                this.mapInstance.addControl(search);
            } else {
                fallbackMap(`Không tìm thấy tọa độ chính xác cho "${query}", đang hiển thị khu vực gần Thái Nguyên.`);
            }
        } catch (error) {
            fallbackMap("Không thể tải bản đồ chính xác, đang hiển thị khu vực gần Thái Nguyên.");
        }
    }

    setupRealtime() {
        if (!this.model.state.user) {
            if (this.model.state.eventSource) {
                this.model.state.eventSource.close();
                this.model.state.eventSource = null;
            }
            return;
        }
        
        // Nếu đã kết nối rồi thì không kết nối lại
        if (this.model.state.eventSource) return;

        const es = new EventSource("/api/events/subscribe");
        this.model.state.eventSource = es;

        es.addEventListener("NEW_MESSAGE", async (event) => {
            try {
                const data = JSON.parse(event.data);
                this.view.showToast(`Tin nhắn mới từ ${data.senderName}`);
                
                // Lấy lại danh sách hội thoại để cập nhật tin nhắn mới nhất
                const convData = await this.model.api("/api/interactions/conversations");
                this.model.state.conversations = convData.conversations ?? [];
                
                // Nếu người dùng đang mở đúng cuộc trò chuyện đó, tải lại chi tiết
                if (this.model.state.activeConversation?.id === data.conversationId) {
                    const activeData = await this.model.api(`/api/interactions/conversations/${data.conversationId}`);
                    this.model.state.activeConversation = activeData.conversation;
                }
                
                this.view.renderConversationPanel(this.model.state);
            } catch (e) {
                console.error("Lỗi xử lý real-time:", e);
            }
        });
    }

    scrollToRoleSectionFromHash() {
        const hash = (window.location.hash || "").replace("#", "");
        const targetId = hash === "tenant" ? "tenantSection" : hash === "landlord" ? "landlordSection" : hash === "admin" ? "adminSection" : null;
        if (targetId) this.view.scrollTo(targetId);
    }

    autoScrollToRole() {
        if (this.model.state.scrolledToRole || !this.model.state.user) return;
        const targetId = this.model.state.user.role === "TENANT" ? "tenantSection" : this.model.state.user.role === "LANDLORD" ? "landlordSection" : this.model.state.user.role === "ADMIN" ? "adminSection" : null;
        if (targetId) {
            this.view.scrollTo(targetId);
            this.model.state.scrolledToRole = true;
        }
    }

    async selectRoom(roomId, scrollIntoView) {
        const data = await this.model.api(`/api/rooms/${roomId}`);
        this.model.state.selectedRoom = data.room;
        this.view.renderDetail(this.model.state);
        this.initLeafletMap(data.room);
        if (scrollIntoView) this.view.scrollTo("detailPanel");
    }

    async onSearch(event) {
        event.preventDefault();
        this.model.state.currentPage = 0;
        const params = Object.fromEntries(new FormData(event.target).entries());
        params.page = 0;
        await this.model.loadRooms(params);
        this.view.renderRooms(this.model.state);
        this.view.renderQuickStats(this.model.state);
    }

    async onPageChange(page) {
        this.model.state.currentPage = page;
        const form = document.getElementById("searchForm");
        const params = form ? Object.fromEntries(new FormData(form).entries()) : {};
        params.page = page;
        await this.model.loadRooms(params);
        this.view.renderRooms(this.model.state);
        this.view.scrollTo("roomSection");
    }

    async onLogout() {
        try {
            if (this.model.state.eventSource) {
                this.model.state.eventSource.close();
                this.model.state.eventSource = null;
            }
            await this.model.api("/api/auth/logout", { method: "POST" });
            this.view.showToast("Đã đăng xuất.");
            window.location.href = "/auth.html?logged_out=1";
        } catch (error) {
            window.location.href = "/auth.html?logged_out=1";
        }
    }

    async onRoomSubmit(event) {
        event.preventDefault();
        const formData = new FormData(event.target);
        const fileList = document.getElementById("imageFiles")?.files;
        
        let uploadedUrls = [];
        if (fileList && fileList.length > 0) {
            const uploadData = new FormData();
            for (let i = 0; i < fileList.length; i++) {
                uploadData.append("files", fileList[i]);
            }
            try {
                this.view.showToast("Đang tải ảnh lên...");
                // Yêu cầu Backend cần có API POST /api/upload nhận multipart/form-data và trả về mảng urls
                const uploadRes = await this.model.api("/api/upload", {
                    method: "POST",
                    body: uploadData
                });
                uploadedUrls = uploadRes.urls || [];
            } catch (error) {
                return this.view.showToast("Tải ảnh thất bại: " + error.message, true);
            }
        }

        const featuredImageInput = formData.get("featuredImage");
        const featuredImageValue = featuredImageInput && featuredImageInput.trim().length ? featuredImageInput : (uploadedUrls.length ? uploadedUrls[0] : "");
        const sizeValue = parseNumber(formData.get("size"));
        const priceValue = Number(formData.get("price"));
        const capacityValue = Number(formData.get("capacity"));

        if (!featuredImageValue && uploadedUrls.length === 0) return this.view.showToast("Vui lòng nhập URL ảnh đại diện hoặc chọn ảnh từ máy.", true);
        if (Number.isNaN(priceValue) || priceValue <= 0) return this.view.showToast("Giá thuê phải là số hợp lệ.", true);
        if (sizeValue === null || sizeValue <= 0) return this.view.showToast("Diện tích phải là số hợp lệ.", true);
        if (Number.isNaN(capacityValue) || capacityValue <= 0) return this.view.showToast("Số phòng hiện tại phải là số hợp lệ.", true);

        const payload = {
            propertyName: formData.get("propertyName"), title: formData.get("title"), address: formData.get("address"),
            areaName: formData.get("areaName"), price: priceValue, size: sizeValue, capacity: capacityValue,
            amenities: splitComma(formData.get("amenities")), featuredImage: featuredImageValue,
            imageUrls: [...splitComma(formData.get("imageUrls")), ...uploadedUrls], description: formData.get("description"),
            contractNote: formData.get("contractNote"), mapQuery: formData.get("mapQuery"), contactPhone: formData.get("contactPhone"),
            status: formData.get("status"), availableFrom: formData.get("availableFrom")
        };
        const path = this.model.state.editingRoomId ? `/api/rooms/${this.model.state.editingRoomId}` : "/api/rooms";
        const method = this.model.state.editingRoomId ? "PUT" : "POST";
        try {
            await this.model.api(path, jsonOptions(payload, method));
            this.view.showToast(this.model.state.editingRoomId ? "Cập nhật phòng thành công." : "Đăng tin thành công.");
            this.resetRoomForm();
            await this.refreshApp();
        } catch (error) {
            this.view.showToast(error.message, true);
        }
    }

    resetRoomForm() {
        this.model.state.editingRoomId = null;
        this.view.resetRoomForm();
    }

    async onSendMessage(event) {
        event.preventDefault();
        if (!this.model.state.activeConversation) return;
        try {
            const payload = { content: this.view.refs.messageInput.value };
            const data = await this.model.api(`/api/interactions/conversations/${this.model.state.activeConversation.id}/messages`, jsonOptions(payload));
            this.model.state.activeConversation = data.conversation;
            this.view.refs.messageInput.value = "";
            const convData = await this.model.api("/api/interactions/conversations");
            this.model.state.conversations = convData.conversations ?? [];
            this.view.renderConversationPanel(this.model.state);
        } catch (error) {
            this.view.showToast(error.message, true);
        }
    }

    async onAskChatbot(event) {
        event.preventDefault();
        const payload = Object.fromEntries(new FormData(event.target).entries());
        try {
            this.model.state.chatbot = await this.model.api("/api/chatbot", jsonOptions(payload));
            this.view.renderChatbot(this.model.state);
        } catch (error) {
            this.view.showToast(error.message, true);
        }
    }

    onClearChatbot(event) {
        event.preventDefault();
        this.model.state.chatbot = null;
        this.view.refs.chatbotForm.reset();
        this.view.renderChatbot(this.model.state);
        this.view.refs.chatbotForm.prompt.focus();
    }

    async onBackup(event) {
        try {
            const data = await this.model.api("/api/dashboard/backup");
            const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = "trototn-backup.json";
            link.click();
            URL.revokeObjectURL(url);
            this.view.showToast("Đã tạo backup JSON.");
        } catch (error) {
            this.view.showToast(error.message, true);
        }
    }

    async onDocumentClick(event) {
        const button = event.target.closest("[data-action]");
        if (!button) return;
        const { action, id, status } = button.dataset;
        try {
            if (action === "view-room") await this.selectRoom(id, true);
            if (action === "toggle-favorite") {
                await this.model.api(`/api/rooms/${id}/favorite`, { method: "POST" });
                await this.refreshApp();
                if (this.model.state.selectedRoom?.id === Number(id)) await this.selectRoom(id, false);
                this.view.showToast("Đã cập nhật yêu thích.");
            }
            if (action === "edit-room") {
                const room = this.model.state.myRooms.find(item => item.id === Number(id));
                if (room) {
                    this.model.state.editingRoomId = room.id;
                    this.view.fillRoomForm(room);
                }
            }
            if (action === "delete-room") {
                if (!confirm("Xóa phòng này? Thao tác này sẽ xóa cả dữ liệu liên quan.")) return;
                await this.model.api(`/api/rooms/${id}`, { method: "DELETE" });
                this.view.showToast("Đã xóa phòng.");
                await this.refreshApp();
            }
            if (action === "update-room-status") {
                await this.model.api(`/api/rooms/${id}/status`, jsonOptions({ status }, "PATCH"));
                this.view.showToast("Đã cập nhật trạng thái phòng.");
                await this.refreshApp();
            }
            if (action === "moderate-room") {
                const note = status === "APPROVED" ? "Tin đủ điều kiện hiển thị." : "Cần bổ sung thông tin nội dung hoặc hình ảnh.";
                await this.model.api(`/api/rooms/${id}/moderation`, jsonOptions({ moderationStatus: status, note }, "PATCH"));
                this.view.showToast("Đã xử lý kiểm duyệt.");
                await this.refreshApp();
            }
            if (action === "user-role") {
                await this.model.api(`/api/users/${id}/role`, jsonOptions({ role: button.dataset.role }, "PATCH"));
                this.view.showToast("Đã cập nhật vai trò người dùng.");
                await this.refreshApp();
            }
            if (action === "toggle-user-list") {
                this.model.state.userListCollapsed = !this.model.state.userListCollapsed;
                this.view.renderAdminSection(this.model.state);
            }
            if (action === "toggle-admin-user-detail") {
                this.model.state.adminUserDetailCollapsed = !this.model.state.adminUserDetailCollapsed;
                this.view.renderAdminUserDetail(this.model.state);
            }
            if (action === "toggle-user-card") {
                const key = Number(id);
                if (this.model.state.collapsedUserCards.has(key)) {
                    this.model.state.collapsedUserCards.delete(key);
                } else {
                    this.model.state.collapsedUserCards.add(key);
                }
                this.view.renderAdminSection(this.model.state);
            }
            if (action === "user-detail") {
                const data = await this.model.api(`/api/users/${id}`);
                this.model.state.selectedUser = data.user;
                this.view.renderAdminUserDetail(this.model.state);
            }
            if (action === "delete-user") {
                const name = button.dataset.name || "tài khoản này";
                if (!confirm(`Xóa ${name}? Thao tác này sẽ xóa cả dữ liệu liên quan.`)) return;
                await this.model.api(`/api/users/${id}`, { method: "DELETE" });
                if (this.model.state.selectedUser?.id === Number(id)) this.model.state.selectedUser = null;
                this.model.state.collapsedUserCards.delete(Number(id));
                this.view.showToast("Đã xóa tài khoản.");
                await this.refreshApp();
            }
            if (action === "user-status") {
                const locked = button.dataset.locked === "true";
                await this.model.api(`/api/users/${id}/status`, jsonOptions({ locked }, "PATCH"));
                this.view.showToast("Đã cập nhật trạng thái tài khoản.");
                await this.refreshApp();
            }
            if (action === "select-conversation") {
                const data = await this.model.api(`/api/interactions/conversations/${id}`);
                this.model.state.activeConversation = data.conversation;
                this.view.renderConversationPanel(this.model.state);
            }
            if (action === "rental-status") {
                await this.model.api(`/api/interactions/rental-requests/${id}`, jsonOptions({ status }, "PATCH"));
                this.view.showToast("Đã cập nhật yêu cầu thuê.");
                await this.refreshApp();
            }
            if (action === "open-contact-form") {
                document.getElementById("conversationStartForm")?.classList.toggle("hidden");
            }
        } catch (error) {
            this.view.showToast(error.message, true);
        }
    }

    async onDynamicSubmit(event) {
        if (event.target.id === "rentalRequestForm") {
            event.preventDefault();
            const roomId = event.target.dataset.roomId;
            const payload = Object.fromEntries(new FormData(event.target).entries());
            try {
                await this.model.api(`/api/interactions/rooms/${roomId}/rental-requests`, jsonOptions(payload));
                this.view.showToast("Đã gửi yêu cầu thuê.");
                event.target.reset();
                await this.refreshApp();
            } catch (error) {
                this.view.showToast(error.message, true);
            }
        }
        if (event.target.id === "surveyForm") {
            event.preventDefault();
            const roomId = event.target.dataset.roomId;
            const payload = Object.fromEntries(new FormData(event.target).entries());
            payload.cleanlinessRating = Number(payload.cleanlinessRating);
            payload.securityRating = Number(payload.securityRating);
            payload.convenienceRating = Number(payload.convenienceRating);
            try {
                const data = await this.model.api(`/api/interactions/rooms/${roomId}/survey`, jsonOptions(payload));
                this.model.state.selectedRoom = data.room;
                this.view.renderDetail(this.model.state);
                this.view.showToast("Đã gửi đánh giá.");
            } catch (error) {
                this.view.showToast(error.message, true);
            }
        }
        if (event.target.id === "conversationStartForm") {
            event.preventDefault();
            const roomId = event.target.dataset.roomId;
            const payload = Object.fromEntries(new FormData(event.target).entries());
            payload.roomId = Number(roomId);
            try {
                const data = await this.model.api("/api/interactions/conversations", jsonOptions(payload));
                this.model.state.activeConversation = data.conversation;
                event.target.reset();
                event.target.classList.add("hidden");
                const convData = await this.model.api("/api/interactions/conversations");
                this.model.state.conversations = convData.conversations ?? [];
                this.view.renderConversationPanel(this.model.state);
                this.view.showToast("Đã tạo cuộc trò chuyện.");
            } catch (error) {
                this.view.showToast(error.message, true);
            }
        }
    }
}

BrowseView.prototype.userItem = function (user) {
    const collapsed = this.model.state.collapsedUserCards.has(Number(user.id));
    const icon = collapsed ? "▸" : "▾";
    const controls = collapsed ? "" : `
                <div class="stack-meta">
                    <span class="muted-badge">${labelRole(user.role)}</span>
                    ${user.locked ? `<span class="muted-badge">Đã khóa</span>` : `<span class="muted-badge">Hoạt động</span>`}
                </div>
                <div class="stack-actions">
                    <button class="ghost-button" type="button" data-action="user-role" data-id="${user.id}" data-role="TENANT">Thuê</button>
                    <button class="ghost-button" type="button" data-action="user-role" data-id="${user.id}" data-role="LANDLORD">Chủ trọ</button>
                    <button class="ghost-button" type="button" data-action="user-role" data-id="${user.id}" data-role="ADMIN">Admin</button>
                    <button class="primary-button" type="button" data-action="user-status" data-id="${user.id}" data-locked="${!user.locked}">${user.locked ? "Mở khóa" : "Khóa"}</button>
                    <button class="ghost-button danger-button" type="button" data-action="delete-user" data-id="${user.id}" data-name="${escapeHtml(user.fullName)}">Xóa</button>
                </div>`;
    return `
            <div class="stack-item">
                <div class="user-row-header">
                    <button class="icon-button collapse-button" type="button" data-action="toggle-user-card" data-id="${user.id}" aria-label="${collapsed ? "Mở rộng" : "Thu gọn"} tài khoản">${icon}</button>
                    <button class="text-button user-name-button" type="button" data-action="user-detail" data-id="${user.id}">
                        ${escapeHtml(user.fullName)} (${escapeHtml(user.email)})
                    </button>
                </div>
                ${controls}
            </div>`;
};

BrowseView.prototype.userItem = function (user) {
    return `
            <div class="stack-item">
                <button class="text-button user-name-button" type="button" data-action="user-detail" data-id="${user.id}">
                    ${escapeHtml(user.fullName)} (${escapeHtml(user.email)})
                </button>
                <div class="stack-meta">
                    <span class="muted-badge">${labelRole(user.role)}</span>
                    ${user.locked ? `<span class="muted-badge">Đã khóa</span>` : `<span class="muted-badge">Hoạt động</span>`}
                </div>
                <div class="stack-actions">
                    <button class="ghost-button" type="button" data-action="user-role" data-id="${user.id}" data-role="TENANT">Thuê</button>
                    <button class="ghost-button" type="button" data-action="user-role" data-id="${user.id}" data-role="LANDLORD">Chủ trọ</button>
                    <button class="ghost-button" type="button" data-action="user-role" data-id="${user.id}" data-role="ADMIN">Admin</button>
                    <button class="primary-button" type="button" data-action="user-status" data-id="${user.id}" data-locked="${!user.locked}">${user.locked ? "Mở khóa" : "Khóa"}</button>
                    <button class="ghost-button danger-button" type="button" data-action="delete-user" data-id="${user.id}" data-name="${escapeHtml(user.fullName)}">Xóa</button>
                </div>
            </div>`;
};

document.addEventListener("DOMContentLoaded", () => {
    new BrowseController(new BrowseModel(), new BrowseView());
});

// ---- Global Utility Functions ----

function jsonOptions(body, method = "POST") {
    return {
        method,
        body: JSON.stringify(body)
    };
}

function splitComma(value) {
    return String(value ?? "")
        .split(",")
        .map(item => item.trim())
        .filter(Boolean);
}

function formatMoney(value) {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value ?? 0));
}

function parseNumber(value) {
    const cleaned = String(value ?? "").replace(/[^0-9.,]/g, "").replace(",", ".");
    return cleaned ? Number(cleaned) : null;
}

function formatDate(value) {
    if (!value) {
        return "Đang cập nhật";
    }
    const date = String(value).includes("T") ? new Date(value) : new Date(`${value}T00:00:00`);
    return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: String(value).includes("T") ? "short" : undefined }).format(date);
}

function formatDateTime(value) {
    return formatDate(value);
}

function labelRole(role) {
    return {
        TENANT: "Người thuê",
        LANDLORD: "Chủ trọ",
        ADMIN: "Admin"
    }[role] ?? role;
}

function labelRoomStatus(status) {
    return {
        AVAILABLE: "Còn trống",
        OCCUPIED: "Đã có người thuê",
        MAINTENANCE: "Đang sửa chữa",
        EXPIRING_SOON: "Sắp hết hợp đồng"
    }[status] ?? status;
}

function labelModeration(status) {
    return {
        PENDING: "Chờ duyệt",
        APPROVED: "Đã duyệt",
        REJECTED: "Bị từ chối"
    }[status] ?? status;
}

function labelRentalStatus(status) {
    return {
        PENDING: "Đang chờ xác nhận",
        APPROVED: "Đã duyệt",
        REJECTED: "Bị từ chối",
        CANCELLED: "Đã hủy"
    }[status] ?? status;
}

function showToast(message, isError = false) {
    const toast = document.getElementById("toast");
    if (!toast) return;
    toast.textContent = message;
    toast.classList.remove("hidden");
    toast.style.background = isError ? "#cfcfcf" : "#ffffff";
    toast.style.color = "#111111";
    toast.style.border = "1px solid #111111";
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.add("hidden"), 3200);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function normalizeMapValue(value) {
    return String(value ?? "").trim();
}

function isHttpUrl(value) {
    return /^https?:\/\//i.test(normalizeMapValue(value));
}

function isMapsUrl(value) {
    const text = normalizeMapValue(value);
    return /^https?:\/\/(www\.)?(google\.[^/]+\/maps|maps\.app\.goo\.gl|goo\.gl\/maps|maps\.google\.[^/]+)/i.test(text);
}

function extractMapCoordinates(value) {
    const text = normalizeMapValue(value);
    if (!text) return null;

    const decoded = (() => {
        try {
            return decodeURIComponent(text);
        } catch {
            return text;
        }
    })();

    const patterns = [
        /@(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)/,
        /[?&](?:q|query|ll)=(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)/,
        /!3d(-?\d+(?:\.\d+)?)!4d(-?\d+(?:\.\d+)?)/,
    ];

    for (const pattern of patterns) {
        const match = decoded.match(pattern);
        if (!match) continue;
        const lat = Number(match[1]);
        const lng = Number(match[2]);
        if (Number.isFinite(lat) && Number.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180) {
            return { lat, lng };
        }
    }

    return null;
}

function buildMapSearchQuery(room) {
    const address = normalizeMapValue(room?.address);
    const mapQuery = normalizeMapValue(room?.mapQuery);
    const areaName = normalizeMapValue(room?.areaName);

    if (address && !isHttpUrl(address)) return address;
    if (mapQuery && !isHttpUrl(mapQuery)) return mapQuery;
    if (areaName) return areaName.includes("Thái Nguyên") ? areaName : `${areaName}, Thái Nguyên`;
    return "";
}

function buildGoogleMapsUrl(room) {
    const address = normalizeMapValue(room?.address);
    const mapQuery = normalizeMapValue(room?.mapQuery);

    if (isMapsUrl(address)) return address;
    if (isMapsUrl(mapQuery)) return mapQuery;

    const query = buildMapSearchQuery(room);
    return query ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}` : "";
}

function displayRoomAddress(room) {
    const address = normalizeMapValue(room?.address);
    if (!isMapsUrl(address)) return address;
    return normalizeMapValue(room?.areaName) || "Link Google Maps";
}
