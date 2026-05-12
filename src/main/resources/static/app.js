const state = {
    user: null,
    rooms: [],
    selectedRoom: null,
    favorites: [],
    myRooms: [],
    conversations: [],
    activeConversation: null,
    rentalRequests: [],
    landlordDashboard: null,
    adminDashboard: null,
    pendingRooms: [],
    editingRoomId: null,
    chatbot: null
};

const refs = {
    userBadge: document.getElementById("userBadge"),
    logoutBtn: document.getElementById("logoutBtn"),
    roomList: document.getElementById("roomList"),
    resultCount: document.getElementById("resultCount"),
    detailContent: document.getElementById("detailContent"),
    authPanel: document.getElementById("authPanel"),
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
    conversationPanel: document.getElementById("conversationPanel"),
    conversationList: document.getElementById("conversationList"),
    messageThread: document.getElementById("messageThread"),
    messageForm: document.getElementById("messageForm"),
    messageInput: document.getElementById("messageInput"),
    chatbotReply: document.getElementById("chatbotReply"),
    roomSubmitBtn: document.getElementById("roomSubmitBtn"),
    roomResetBtn: document.getElementById("roomResetBtn"),
    roomForm: document.getElementById("roomForm"),
    toast: document.getElementById("toast"),
    statRooms: document.getElementById("statRooms"),
    statPending: document.getElementById("statPending")
};

document.addEventListener("DOMContentLoaded", init);

async function init() {
    bindStaticEvents();
    await refreshApp();
}

function bindStaticEvents() {
    document.getElementById("searchForm").addEventListener("submit", onSearch);
    document.getElementById("loginForm").addEventListener("submit", onLogin);
    document.getElementById("registerForm").addEventListener("submit", onRegister);
    refs.logoutBtn.addEventListener("click", onLogout);
    refs.roomForm.addEventListener("submit", onRoomSubmit);
    refs.roomResetBtn.addEventListener("click", resetRoomForm);
    refs.messageForm.addEventListener("submit", onSendMessage);
    document.getElementById("chatbotForm").addEventListener("submit", onAskChatbot);
    document.getElementById("backupBtn").addEventListener("click", onBackup);
    document.addEventListener("click", onDocumentClick);
    document.addEventListener("submit", onDynamicSubmit);
}

async function refreshApp() {
    const selectedId = state.selectedRoom?.id ?? null;
    state.user = null;
    state.rooms = [];
    state.favorites = [];
    state.myRooms = [];
    state.conversations = [];
    state.activeConversation = null;
    state.rentalRequests = [];
    state.landlordDashboard = null;
    state.adminDashboard = null;
    state.pendingRooms = [];

    await loadCurrentUser();
    await loadRooms();

    const tasks = [];
    if (state.user) {
        tasks.push(loadConversations(), loadRentalRequests());
        if (state.user.role === "TENANT") {
            tasks.push(loadFavorites());
        }
        if (state.user.role === "LANDLORD") {
            tasks.push(loadMyRooms(), loadLandlordDashboard());
        }
        if (state.user.role === "ADMIN") {
            tasks.push(loadPendingRooms(), loadAdminDashboard());
        }
    }
    await Promise.all(tasks);
    renderAll();

    if (selectedId) {
        await selectRoom(selectedId, false);
    }
}

async function loadCurrentUser() {
    const data = await api("/api/auth/me");
    state.user = data.user;
}

async function loadRooms(params = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== null && value !== undefined && String(value).trim() !== "") {
            query.append(key, value);
        }
    });
    const suffix = query.toString() ? `?${query.toString()}` : "";
    const data = await api(`/api/rooms${suffix}`);
    state.rooms = data.rooms ?? [];
}

async function loadFavorites() {
    const data = await api("/api/rooms/favorites");
    state.favorites = data.rooms ?? [];
}

async function loadMyRooms() {
    const data = await api("/api/rooms/mine");
    state.myRooms = data.rooms ?? [];
}

async function loadConversations() {
    const data = await api("/api/interactions/conversations");
    state.conversations = data.conversations ?? [];
}

async function loadRentalRequests() {
    const data = await api("/api/interactions/rental-requests");
    state.rentalRequests = data.rentalRequests ?? [];
}

async function loadLandlordDashboard() {
    const data = await api("/api/dashboard/landlord");
    state.landlordDashboard = data.dashboard;
}

async function loadAdminDashboard() {
    const data = await api("/api/dashboard/admin");
    state.adminDashboard = data.dashboard;
}

async function loadPendingRooms() {
    const data = await api("/api/rooms/pending");
    state.pendingRooms = data.rooms ?? [];
}

function renderAll() {
    renderUser();
    renderQuickStats();
    renderRooms();
    renderAuthPanel();
    renderTenantSection();
    renderLandlordSection();
    renderAdminSection();
    renderConversationPanel();
    renderChatbot();
    renderDetail();
}

function renderUser() {
    if (!state.user) {
        refs.userBadge.textContent = "Chua dang nhap";
        refs.logoutBtn.classList.add("hidden");
        return;
    }
    refs.userBadge.textContent = `${state.user.fullName} · ${labelRole(state.user.role)}`;
    refs.logoutBtn.classList.remove("hidden");
}

function renderQuickStats() {
    refs.statRooms.textContent = state.rooms.length;
    refs.statPending.textContent = state.adminDashboard?.pendingRooms ?? state.pendingRooms.length ?? 0;
}

function renderRooms() {
    refs.resultCount.textContent = `${state.rooms.length} ket qua`;
    if (!state.rooms.length) {
        refs.roomList.innerHTML = `<div class="empty-state">Khong tim thay phong phu hop. Thu mo rong bo loc hoac thay doi khu vuc.</div>`;
        return;
    }
    refs.roomList.innerHTML = state.rooms.map(roomCard).join("");
}

function renderAuthPanel() {
    if (!state.user) {
        refs.authPanel.innerHTML = `
            <div class="section-header">
                <div>
                    <p class="eyebrow">Tai khoan</p>
                    <h3>Dang nhap / Dang ky</h3>
                </div>
            </div>
            <div class="auth-grid">
                <form id="loginForm" class="stack-form">
                    <h4>Dang nhap</h4>
                    <input name="email" type="email" placeholder="Email" required>
                    <input name="password" type="password" placeholder="Mat khau" required>
                    <button class="primary-button" type="submit">Dang nhap</button>
                </form>
                <form id="registerForm" class="stack-form">
                    <h4>Dang ky</h4>
                    <input name="fullName" placeholder="Ho va ten" required>
                    <input name="email" type="email" placeholder="Email" required>
                    <input name="password" type="password" placeholder="Mat khau" required>
                    <input name="phone" placeholder="So dien thoai">
                    <input name="address" placeholder="Dia chi">
                    <select name="role">
                        <option value="TENANT">Nguoi thue</option>
                        <option value="LANDLORD">Chu tro</option>
                    </select>
                    <button class="ghost-button" type="submit">Tao tai khoan</button>
                </form>
            </div>
        `;
        refs.authPanel.querySelector("#loginForm").addEventListener("submit", onLogin);
        refs.authPanel.querySelector("#registerForm").addEventListener("submit", onRegister);
        return;
    }
    refs.authPanel.innerHTML = `
        <div class="section-header">
            <div>
                <p class="eyebrow">Phien dang nhap</p>
                <h3>${escapeHtml(state.user.fullName)}</h3>
            </div>
        </div>
        <div class="stack-item">
            <p><strong>Vai tro:</strong> ${labelRole(state.user.role)}</p>
            <p><strong>Email:</strong> ${escapeHtml(state.user.email ?? "")}</p>
            <p><strong>So dien thoai:</strong> ${escapeHtml(state.user.phone ?? "") || "Dang cap nhat"}</p>
            <p><strong>Dia chi:</strong> ${escapeHtml(state.user.address ?? "") || "Dang cap nhat"}</p>
        </div>
        <div class="empty-state">Ban dang su dung giao dien role-based. Moi thao tac se cap nhat thong tin trong cac panel ben duoi.</div>
    `;
}

function renderTenantSection() {
    const enabled = state.user?.role === "TENANT";
    refs.tenantSection.classList.toggle("hidden", !enabled);
    if (!enabled) {
        return;
    }
    refs.favoriteList.innerHTML = state.favorites.length
        ? state.favorites.map(stackRoomItem).join("")
        : `<div class="empty-state">Chua co phong yeu thich.</div>`;
    refs.tenantRentalList.innerHTML = state.rentalRequests.length
        ? state.rentalRequests.map(rentalItem).join("")
        : `<div class="empty-state">Ban chua gui yeu cau thue nao.</div>`;
}

function renderLandlordSection() {
    const enabled = state.user?.role === "LANDLORD";
    refs.landlordSection.classList.toggle("hidden", !enabled);
    if (!enabled) {
        return;
    }
    refs.landlordStats.innerHTML = state.landlordDashboard ? statTiles([
        ["Tong phong", state.landlordDashboard.totalRooms],
        ["Con trong", state.landlordDashboard.availableRooms],
        ["Da thue", state.landlordDashboard.occupiedRooms],
        ["Dang sua", state.landlordDashboard.maintenanceRooms],
        ["Sap het HD", state.landlordDashboard.expiringSoonRooms],
        ["Luot xem", state.landlordDashboard.totalViews],
        ["Lien he", state.landlordDashboard.totalContacts],
        ["Cho duyet", state.landlordDashboard.pendingModeration]
    ]) : "";

    refs.myRoomList.innerHTML = state.myRooms.length
        ? state.myRooms.map(landlordRoomCard).join("")
        : `<div class="empty-state">Ban chua co tin dang nao.</div>`;

    refs.landlordConversationList.innerHTML = state.conversations.length
        ? state.conversations.map(conversationItem).join("")
        : `<div class="empty-state">Chua co hoi thoai nao.</div>`;

    refs.landlordRentalList.innerHTML = state.rentalRequests.length
        ? state.rentalRequests.map(rentalItem).join("")
        : `<div class="empty-state">Chua co yeu cau thue nao.</div>`;
}

function renderAdminSection() {
    const enabled = state.user?.role === "ADMIN";
    refs.adminSection.classList.toggle("hidden", !enabled);
    if (!enabled) {
        return;
    }
    refs.adminStats.innerHTML = state.adminDashboard ? statTiles([
        ["Tong user", state.adminDashboard.totalUsers],
        ["Nguoi thue", state.adminDashboard.tenantCount],
        ["Chu tro", state.adminDashboard.landlordCount],
        ["Phong duyet", state.adminDashboard.approvedRooms],
        ["Cho duyet", state.adminDashboard.pendingRooms],
        ["Da thue", state.adminDashboard.occupiedRooms],
        ["Luot xem", state.adminDashboard.totalViews],
        ["Hoi thoai", state.adminDashboard.totalConversations]
    ]) : "";

    refs.pendingRoomList.innerHTML = state.pendingRooms.length
        ? state.pendingRooms.map(adminRoomItem).join("")
        : `<div class="empty-state">Khong con tin dang cho duyet.</div>`;

    refs.adminRentalList.innerHTML = state.adminDashboard?.recentRentalRequests?.length
        ? state.adminDashboard.recentRentalRequests.map(reportRentalItem).join("")
        : `<div class="empty-state">Chua co du lieu yeu cau thue.</div>`;

    refs.monthlyReport.innerHTML = reportList(state.adminDashboard?.monthlyReport ?? []);
    refs.quarterYearReport.innerHTML = `${reportList(state.adminDashboard?.quarterlyReport ?? [])}${reportList(state.adminDashboard?.yearlyReport ?? [])}`;
}

function renderConversationPanel() {
    const enabled = Boolean(state.user);
    refs.conversationPanel.classList.toggle("hidden", !enabled);
    if (!enabled) {
        return;
    }
    refs.conversationList.innerHTML = state.conversations.length
        ? state.conversations.map(conversationItem).join("")
        : `<div class="empty-state">Chua co cuoc tro chuyen nao.</div>`;

    if (!state.activeConversation) {
        refs.messageThread.innerHTML = `<div class="empty-state">Chon cuoc tro chuyen de xem noi dung.</div>`;
        refs.messageForm.classList.add("hidden");
        return;
    }

    refs.messageThread.innerHTML = state.activeConversation.messages.map(message => `
        <div class="message-bubble ${message.senderId === state.user.id ? "mine" : ""}">
            <strong>${escapeHtml(message.senderName)}</strong>
            <p>${escapeHtml(message.content)}</p>
            <small>${formatDate(message.createdAt)}</small>
        </div>
    `).join("");
    refs.messageForm.classList.remove("hidden");
}

function renderChatbot() {
    if (!state.chatbot) {
        refs.chatbotReply.innerHTML = `Chatbot se phan tich ngan sach, khu vuc va tien nghi de goi y phong.`;
        return;
    }
    const suggestions = state.chatbot.suggestions?.length
        ? `<div class="stack-list">${state.chatbot.suggestions.map(stackRoomItem).join("")}</div>`
        : "";
    refs.chatbotReply.innerHTML = `
        <div class="stack-item">
            <p><strong>Tra loi:</strong> ${escapeHtml(state.chatbot.reply)}</p>
            <div class="stack-meta">
                ${state.chatbot.budget ? `<span class="muted-badge">Ngan sach: ${formatMoney(state.chatbot.budget)}</span>` : ""}
                ${state.chatbot.area ? `<span class="muted-badge">Khu vuc: ${escapeHtml(state.chatbot.area)}</span>` : ""}
                ${state.chatbot.amenity ? `<span class="muted-badge">Tien nghi: ${escapeHtml(state.chatbot.amenity)}</span>` : ""}
            </div>
        </div>
        ${suggestions}
    `;
}

function renderDetail() {
    if (!state.selectedRoom) {
        refs.detailContent.innerHTML = `Chi tiet phong, anh, map, danh gia va thao tac se hien tai day.`;
        return;
    }
    const room = state.selectedRoom;
    const tenantActions = state.user?.role === "TENANT" ? `
        <div class="card-actions">
            <button class="primary-button" type="button" data-action="toggle-favorite" data-id="${room.id}">${room.favorite ? "Bo yeu thich" : "Luu yeu thich"}</button>
            <button class="ghost-button" type="button" data-action="open-contact-form" data-id="${room.id}">Nhan tin chu tro</button>
        </div>
        <form id="conversationStartForm" class="stack-form detail-block hidden" data-room-id="${room.id}">
            <textarea name="content" placeholder="Nhap noi dung muon trao doi voi chu tro..." required></textarea>
            <button class="primary-button" type="submit">Bat dau chat</button>
        </form>
        <form id="rentalRequestForm" class="stack-form detail-block" data-room-id="${room.id}">
            <h4>Gui yeu cau thue</h4>
            <input name="moveInDate" type="date" required>
            <textarea name="note" placeholder="Mo ta nhu cau, thoi gian vao o, thoi han hop dong..." required></textarea>
            <button class="primary-button" type="submit">Gui yeu cau thue</button>
        </form>
        <form id="surveyForm" class="stack-form detail-block" data-room-id="${room.id}">
            <h4>Danh gia phong / nha tro</h4>
            <div class="split-two">
                <input name="cleanlinessRating" type="number" min="1" max="5" placeholder="Ve sinh (1-5)" required>
                <input name="securityRating" type="number" min="1" max="5" placeholder="An ninh (1-5)" required>
            </div>
            <input name="convenienceRating" type="number" min="1" max="5" placeholder="Tien nghi (1-5)" required>
            <textarea name="comment" placeholder="Cam nhan cua ban..." required></textarea>
            <button class="ghost-button" type="submit">Gui danh gia</button>
        </form>
    ` : "";

    refs.detailContent.innerHTML = `
        <div class="detail-hero">
            <img src="${escapeHtml(room.featuredImage)}" alt="${escapeHtml(room.title)}">
        </div>
        <div class="detail-block">
            <div class="price-line">
                <h3>${escapeHtml(room.title)}</h3>
                <span class="price-chip">${formatMoney(room.price)}/thang</span>
            </div>
            <p class="muted-text">${escapeHtml(room.propertyName)} · ${escapeHtml(room.address)}</p>
            <div class="tag-row">
                <span class="status-chip">${labelRoomStatus(room.status)}</span>
                <span class="muted-badge">${room.size} m2</span>
                <span class="muted-badge">${room.capacity} nguoi</span>
                <span class="muted-badge">${room.viewCount} luot xem</span>
                <span class="muted-badge">${room.contactCount} lien he</span>
                <span class="muted-badge">Danh gia ${room.surveyAverage} (${room.surveyCount})</span>
            </div>
            <p>${escapeHtml(room.description)}</p>
            <div class="tag-row">${(room.amenities ?? []).map(amenity => `<span class="tag">${escapeHtml(amenity)}</span>`).join("")}</div>
        </div>
        <div class="gallery">${(room.imageUrls ?? []).map(url => `<img src="${escapeHtml(url)}" alt="Anh phong">`).join("")}</div>
        <div class="detail-block">
            <div class="stack-item">
                <p><strong>Chu tro:</strong> ${escapeHtml(room.ownerName)}</p>
                <p><strong>Lien he:</strong> ${escapeHtml(room.contactPhone ?? room.ownerPhone ?? "")}</p>
                <p><strong>Hop dong:</strong> ${escapeHtml(room.contractNote ?? "Dang cap nhat")}</p>
                <p><strong>Co the vao o:</strong> ${formatDate(room.availableFrom)}</p>
            </div>
            <iframe loading="lazy" src="https://www.google.com/maps?q=${encodeURIComponent(room.mapQuery ?? room.address)}&output=embed"></iframe>
        </div>
        ${tenantActions}
        <div class="detail-block">
            <h4>Danh gia gan day</h4>
            ${(room.surveys ?? []).length ? room.surveys.map(survey => `
                <div class="stack-item">
                    <strong>${escapeHtml(survey.userName)}</strong>
                    <div class="stack-meta">
                        <span class="muted-badge">Ve sinh ${survey.cleanlinessRating}/5</span>
                        <span class="muted-badge">An ninh ${survey.securityRating}/5</span>
                        <span class="muted-badge">Tien nghi ${survey.convenienceRating}/5</span>
                    </div>
                    <p>${escapeHtml(survey.comment)}</p>
                </div>
            `).join("") : `<div class="empty-state">Chua co danh gia nao.</div>`}
        </div>
    `;
}

function roomCard(room) {
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
                    <span class="muted-badge">${room.size} m2</span>
                    <span class="muted-badge">${room.capacity} nguoi</span>
                </div>
                <div class="tag-row">${(room.amenities ?? []).slice(0, 4).map(amenity => `<span class="tag">${escapeHtml(amenity)}</span>`).join("")}</div>
                <div class="stack-meta">
                    <span class="muted-badge">${room.viewCount} xem</span>
                    <span class="muted-badge">${room.contactCount} lien he</span>
                    <span class="muted-badge">${room.surveyAverage} sao</span>
                </div>
                <div class="card-actions">
                    <button class="primary-button" type="button" data-action="view-room" data-id="${room.id}">Xem chi tiet</button>
                    ${state.user?.role === "TENANT" ? `<button class="ghost-button" type="button" data-action="toggle-favorite" data-id="${room.id}">${room.favorite ? "Bo luu" : "Yeu thich"}</button>` : ""}
                </div>
            </div>
        </article>
    `;
}

function landlordRoomCard(room) {
    return `
        <article class="room-card">
            <img src="${escapeHtml(room.featuredImage)}" alt="${escapeHtml(room.title)}">
            <div class="room-body">
                <div class="price-line">
                    <strong>${escapeHtml(room.title)}</strong>
                    <span class="price-chip">${formatMoney(room.price)}</span>
                </div>
                <p class="muted-text">${escapeHtml(room.address)}</p>
                <div class="tag-row">
                    <span class="status-chip">${labelRoomStatus(room.status)}</span>
                    <span class="muted-badge">${labelModeration(room.moderationStatus)}</span>
                    <span class="muted-badge">${room.viewCount} xem</span>
                </div>
                <div class="card-actions">
                    <button class="primary-button" type="button" data-action="view-room" data-id="${room.id}">Chi tiet</button>
                    <button class="ghost-button" type="button" data-action="edit-room" data-id="${room.id}">Chinh sua</button>
                    <button class="ghost-button" type="button" data-action="update-room-status" data-id="${room.id}" data-status="AVAILABLE">Con trong</button>
                    <button class="ghost-button" type="button" data-action="update-room-status" data-id="${room.id}" data-status="OCCUPIED">Da thue</button>
                    <button class="ghost-button" type="button" data-action="delete-room" data-id="${room.id}">Xoa</button>
                </div>
            </div>
        </article>
    `;
}

function adminRoomItem(room) {
    return `
        <div class="stack-item">
            <strong>${escapeHtml(room.title)}</strong>
            <p>${escapeHtml(room.propertyName)} · ${escapeHtml(room.address)}</p>
            <div class="stack-meta">
                <span class="muted-badge">${labelRoomStatus(room.status)}</span>
                <span class="muted-badge">${formatMoney(room.price)}</span>
            </div>
            <div class="stack-actions">
                <button class="primary-button" type="button" data-action="view-room" data-id="${room.id}">Xem</button>
                <button class="ghost-button" type="button" data-action="moderate-room" data-id="${room.id}" data-status="APPROVED">Duyet</button>
                <button class="ghost-button" type="button" data-action="moderate-room" data-id="${room.id}" data-status="REJECTED">Tu choi</button>
            </div>
        </div>
    `;
}

function stackRoomItem(room) {
    return `
        <div class="stack-item">
            <strong>${escapeHtml(room.title)}</strong>
            <p>${escapeHtml(room.propertyName)} · ${escapeHtml(room.areaName)}</p>
            <div class="stack-meta">
                <span class="muted-badge">${formatMoney(room.price)}</span>
                <span class="muted-badge">${room.size} m2</span>
            </div>
            <div class="stack-actions">
                <button class="primary-button" type="button" data-action="view-room" data-id="${room.id}">Xem chi tiet</button>
            </div>
        </div>
    `;
}

function rentalItem(item) {
    const canApprove = state.user?.role === "LANDLORD" || state.user?.role === "ADMIN";
    const canCancel = state.user?.role === "TENANT" && item.status === "PENDING";
    return `
        <div class="stack-item">
            <strong>${escapeHtml(item.room.title)}</strong>
            <p>Ngay vao o: ${formatDate(item.moveInDate)}</p>
            <p>${escapeHtml(item.note)}</p>
            <div class="stack-meta">
                <span class="muted-badge">${escapeHtml(item.tenant.fullName)}</span>
                <span class="muted-badge">${escapeHtml(item.landlord.fullName)}</span>
                <span class="muted-badge">${labelRentalStatus(item.status)}</span>
            </div>
            <div class="stack-actions">
                <button class="primary-button" type="button" data-action="view-room" data-id="${item.room.id}">Xem phong</button>
                ${canApprove ? `<button class="ghost-button" type="button" data-action="rental-status" data-id="${item.id}" data-status="APPROVED">Duyet</button>
                <button class="ghost-button" type="button" data-action="rental-status" data-id="${item.id}" data-status="REJECTED">Tu choi</button>` : ""}
                ${canCancel ? `<button class="ghost-button" type="button" data-action="rental-status" data-id="${item.id}" data-status="CANCELLED">Huy yeu cau</button>` : ""}
            </div>
        </div>
    `;
}

function reportRentalItem(item) {
    return `
        <div class="stack-item compact">
            <strong>${escapeHtml(item.roomTitle)}</strong>
            <p>${escapeHtml(item.tenantName)} ↔ ${escapeHtml(item.landlordName)}</p>
            <div class="stack-meta">
                <span class="muted-badge">${labelRentalStatus(item.status)}</span>
                <span class="muted-badge">${formatDate(item.updatedAt)}</span>
            </div>
        </div>
    `;
}

function conversationItem(item) {
    const partner = state.user?.role === "TENANT" ? item.landlord.fullName : item.tenant.fullName;
    return `
        <div class="stack-item compact">
            <strong>${escapeHtml(item.room.title)}</strong>
            <p>${escapeHtml(partner)}</p>
            <div class="stack-meta">
                <span class="muted-badge">${formatDate(item.updatedAt)}</span>
            </div>
            <div class="stack-actions">
                <button class="ghost-button" type="button" data-action="select-conversation" data-id="${item.id}">Mo chat</button>
                <button class="primary-button" type="button" data-action="view-room" data-id="${item.room.id}">Phong</button>
            </div>
        </div>
    `;
}

function reportList(items) {
    if (!items.length) {
        return `<div class="empty-state">Chua co du lieu.</div>`;
    }
    return items.map(item => `
        <div class="stack-item compact">
            <strong>${escapeHtml(item.label)}</strong>
            <p>${item.value} luot</p>
        </div>
    `).join("");
}

function statTiles(items) {
    return items.map(([label, value]) => `
        <div class="stat-tile">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(String(value))}</strong>
        </div>
    `).join("");
}

async function onSearch(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const params = Object.fromEntries(formData.entries());
    await loadRooms(params);
    renderRooms();
    renderQuickStats();
}

async function onLogin(event) {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(event.target).entries());
    try {
        await api("/api/auth/login", jsonOptions(payload));
        event.target.reset();
        showToast("Dang nhap thanh cong.");
        await refreshApp();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onRegister(event) {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(event.target).entries());
    try {
        await api("/api/auth/register", jsonOptions(payload));
        event.target.reset();
        showToast("Dang ky thanh cong.");
        await refreshApp();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onLogout() {
    try {
        await api("/api/auth/logout", { method: "POST" });
        state.selectedRoom = null;
        showToast("Da dang xuat.");
        await refreshApp();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onRoomSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const payload = {
        propertyName: formData.get("propertyName"),
        title: formData.get("title"),
        address: formData.get("address"),
        areaName: formData.get("areaName"),
        price: Number(formData.get("price")),
        size: Number(formData.get("size")),
        capacity: Number(formData.get("capacity")),
        amenities: splitComma(formData.get("amenities")),
        featuredImage: formData.get("featuredImage"),
        imageUrls: splitComma(formData.get("imageUrls")),
        description: formData.get("description"),
        contractNote: formData.get("contractNote"),
        mapQuery: formData.get("mapQuery"),
        contactPhone: formData.get("contactPhone"),
        status: formData.get("status"),
        availableFrom: formData.get("availableFrom")
    };
    const path = state.editingRoomId ? `/api/rooms/${state.editingRoomId}` : "/api/rooms";
    const method = state.editingRoomId ? "PUT" : "POST";
    try {
        await api(path, jsonOptions(payload, method));
        showToast(state.editingRoomId ? "Cap nhat phong thanh cong." : "Dang tin thanh cong.");
        resetRoomForm();
        await refreshApp();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onSendMessage(event) {
    event.preventDefault();
    if (!state.activeConversation) {
        return;
    }
    try {
        const payload = { content: refs.messageInput.value };
        const data = await api(`/api/interactions/conversations/${state.activeConversation.id}/messages`, jsonOptions(payload));
        state.activeConversation = data.conversation;
        refs.messageInput.value = "";
        await loadConversations();
        renderConversationPanel();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onAskChatbot(event) {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(event.target).entries());
    try {
        state.chatbot = await api("/api/chatbot", jsonOptions(payload));
        renderChatbot();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onBackup() {
    try {
        const data = await api("/api/dashboard/backup");
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "trototn-backup.json";
        link.click();
        URL.revokeObjectURL(url);
        showToast("Da tao backup JSON.");
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onDocumentClick(event) {
    const button = event.target.closest("[data-action]");
    if (!button) {
        return;
    }

    const { action, id, status } = button.dataset;
    try {
        if (action === "view-room") {
            await selectRoom(id, true);
        }
        if (action === "toggle-favorite") {
            await api(`/api/rooms/${id}/favorite`, { method: "POST" });
            await refreshApp();
            if (state.selectedRoom?.id === Number(id)) {
                await selectRoom(id, false);
            }
            showToast("Da cap nhat yeu thich.");
        }
        if (action === "edit-room") {
            const room = state.myRooms.find(item => item.id === Number(id));
            if (room) {
                fillRoomForm(room);
            }
        }
        if (action === "delete-room") {
            await api(`/api/rooms/${id}`, { method: "DELETE" });
            showToast("Da xoa phong.");
            await refreshApp();
        }
        if (action === "update-room-status") {
            await api(`/api/rooms/${id}/status`, jsonOptions({ status }, "PATCH"));
            showToast("Da cap nhat trang thai phong.");
            await refreshApp();
        }
        if (action === "moderate-room") {
            const note = status === "APPROVED" ? "Tin du dieu kien hien thi." : "Can bo sung thong tin noi dung hoac hinh anh.";
            await api(`/api/rooms/${id}/moderation`, jsonOptions({ moderationStatus: status, note }, "PATCH"));
            showToast("Da xu ly kiem duyet.");
            await refreshApp();
        }
        if (action === "select-conversation") {
            const data = await api(`/api/interactions/conversations/${id}`);
            state.activeConversation = data.conversation;
            renderConversationPanel();
        }
        if (action === "rental-status") {
            await api(`/api/interactions/rental-requests/${id}`, jsonOptions({ status }, "PATCH"));
            showToast("Da cap nhat yeu cau thue.");
            await refreshApp();
        }
        if (action === "open-contact-form") {
            document.getElementById("conversationStartForm")?.classList.toggle("hidden");
        }
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onDynamicSubmit(event) {
    if (event.target.id === "rentalRequestForm") {
        event.preventDefault();
        const roomId = event.target.dataset.roomId;
        const payload = Object.fromEntries(new FormData(event.target).entries());
        try {
            await api(`/api/interactions/rooms/${roomId}/rental-requests`, jsonOptions(payload));
            showToast("Da gui yeu cau thue.");
            event.target.reset();
            await refreshApp();
        } catch (error) {
            showToast(error.message, true);
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
            const data = await api(`/api/interactions/rooms/${roomId}/survey`, jsonOptions(payload));
            state.selectedRoom = data.room;
            renderDetail();
            showToast("Da gui danh gia.");
        } catch (error) {
            showToast(error.message, true);
        }
    }
    if (event.target.id === "conversationStartForm") {
        event.preventDefault();
        const roomId = event.target.dataset.roomId;
        const payload = Object.fromEntries(new FormData(event.target).entries());
        payload.roomId = Number(roomId);
        try {
            const data = await api("/api/interactions/conversations", jsonOptions(payload));
            state.activeConversation = data.conversation;
            event.target.reset();
            event.target.classList.add("hidden");
            await loadConversations();
            renderConversationPanel();
            showToast("Da tao cuoc tro chuyen.");
        } catch (error) {
            showToast(error.message, true);
        }
    }
}

async function selectRoom(roomId, scrollIntoView) {
    const data = await api(`/api/rooms/${roomId}`);
    state.selectedRoom = data.room;
    renderDetail();
    if (scrollIntoView) {
        document.getElementById("detailPanel").scrollIntoView({ behavior: "smooth", block: "start" });
    }
}

function fillRoomForm(room) {
    state.editingRoomId = room.id;
    refs.roomSubmitBtn.textContent = "Cap nhat phong";
    const form = refs.roomForm;
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
    refs.roomForm.scrollIntoView({ behavior: "smooth", block: "start" });
}

function resetRoomForm() {
    state.editingRoomId = null;
    refs.roomSubmitBtn.textContent = "Dang tin / Cap nhat";
    refs.roomForm.reset();
}

async function api(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json",
            ...(options.headers ?? {})
        },
        ...options
    });

    let data = {};
    try {
        data = await response.json();
    } catch (error) {
        data = {};
    }

    if (!response.ok) {
        throw new Error(data.message || data.error || "Yeu cau that bai.");
    }
    return data;
}

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

function formatDate(value) {
    if (!value) {
        return "Dang cap nhat";
    }
    const date = String(value).includes("T") ? new Date(value) : new Date(`${value}T00:00:00`);
    return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: String(value).includes("T") ? "short" : undefined }).format(date);
}

function labelRole(role) {
    return {
        TENANT: "Nguoi thue",
        LANDLORD: "Chu tro",
        ADMIN: "Admin"
    }[role] ?? role;
}

function labelRoomStatus(status) {
    return {
        AVAILABLE: "Con trong",
        OCCUPIED: "Da co nguoi thue",
        MAINTENANCE: "Dang sua chua",
        EXPIRING_SOON: "Sap het hop dong"
    }[status] ?? status;
}

function labelModeration(status) {
    return {
        PENDING: "Cho duyet",
        APPROVED: "Da duyet",
        REJECTED: "Bi tu choi"
    }[status] ?? status;
}

function labelRentalStatus(status) {
    return {
        PENDING: "Dang cho xac nhan",
        APPROVED: "Da duyet",
        REJECTED: "Bi tu choi",
        CANCELLED: "Da huy"
    }[status] ?? status;
}

function showToast(message, isError = false) {
    refs.toast.textContent = message;
    refs.toast.classList.remove("hidden");
    refs.toast.style.background = isError ? "#8d2f23" : "#1f2530";
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => refs.toast.classList.add("hidden"), 3200);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}
