class RoomDetailPage {
    constructor() {
        this.roomId = new URLSearchParams(window.location.search).get("id");
        this.room = null;
        this.layout = null;
        this.user = null;
        this.detail = document.getElementById("roomDetail");
        this.toast = document.getElementById("toast");
        this.init();
    }

    async init() {
        this.bindEvents();
        if (!this.roomId || !/^\d+$/.test(this.roomId)) {
            this.renderError("Đường dẫn phòng không hợp lệ.");
            return;
        }

        await this.loadUser();
        this.renderUser();
        await this.loadRoom();
    }

    bindEvents() {
        document.addEventListener("click", event => this.onClick(event));
        document.addEventListener("submit", event => this.onSubmit(event));
        document.addEventListener("keydown", event => {
            if (event.key === "Escape") closeImageLightbox();
        });
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
        if (!response.ok) throw new Error(data.message || data.error || "Yêu cầu thất bại.");
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

    async loadRoom() {
        try {
            const [roomData, layoutData] = await Promise.all([
                this.api(`/api/rooms/${this.roomId}`),
                this.api(`/api/rooms/${this.roomId}/layout`).catch(() => null)
            ]);
            this.room = roomData.room;
            this.layout = layoutData?.layout || null;
            this.render();
        } catch (error) {
            this.renderError(error.message || "Không tìm thấy phòng.");
        }
    }

    renderUser() {
        const authLink = document.getElementById("authLink");
        const logoutBtn = document.getElementById("logoutBtn");
        if (!this.user) return;
        authLink.textContent = this.user.fullName;
        authLink.href = "/profile.html";
        logoutBtn.classList.remove("hidden");
    }

    render() {
        const room = this.room;
        const address = displayAddress(room);
        const images = uniqueImages([room.featuredImage, ...(room.imageUrls || [])]);
        const mapQuery = buildMapSearchQuery(room);
        const canRequest = ["AVAILABLE", "EXPIRING_SOON"].includes(room.status);
        const isTenant = this.user?.role === "TENANT";

        document.title = `${room.title} | Trọ Tốt ICTU`;
        document.getElementById("breadcrumbTitle").textContent = room.title;

        this.detail.innerHTML = `
            <section class="room-detail-gallery panel">
                <div class="room-detail-main-image">
                    <button type="button" class="image-view-button" data-action="open-image" data-url="${escapeHtml(imageSrc(images[0]))}" aria-label="Xem ảnh lớn">
                        <img src="${escapeHtml(imageSrc(images[0]))}" alt="${escapeHtml(room.title)}" onerror="${imageFallback()}">
                    </button>
                </div>
                <div class="room-detail-thumbnails">
                    ${images.slice(1, 5).map(url => `
                        <button type="button" data-action="select-image" data-url="${escapeHtml(url)}">
                            <img src="${escapeHtml(imageSrc(url))}" alt="Ảnh ${escapeHtml(room.title)}" onerror="${imageFallback()}">
                        </button>`).join("")}
                </div>
            </section>

            <div class="room-detail-layout">
                <div class="room-detail-main">
                    <section class="panel room-detail-heading">
                        <div class="price-line">
                            <div>
                                <p class="eyebrow">${escapeHtml(room.propertyName || "Nhà trọ")}</p>
                                <h1>${escapeHtml(room.title)}</h1>
                            </div>
                            <span class="price-chip room-detail-price">${formatMoney(room.price)}/tháng</span>
                        </div>
                        <p class="room-detail-address">${escapeHtml(address)}</p>
                        <div class="tag-row">
                            <span class="status-chip">${labelRoomStatus(room.status)}</span>
                            <span class="muted-badge">${displayValue(room.size)} m²</span>
                            <span class="muted-badge">${displayValue(room.capacity)} người</span>
                            <span class="muted-badge">${displayValue(room.availableRooms, 0)}/${displayValue(room.totalRooms, 1)} phòng trống</span>
                            <span class="muted-badge">${displayValue(room.viewCount, 0)} lượt xem</span>
                            <span class="muted-badge">Đánh giá ${room.surveyAverage ?? "Chưa có"} (${room.surveyCount ?? 0})</span>
                        </div>
                        <div class="card-actions detail-layout-action">
                            <a class="primary-button link-button" href="/room-layout.html?id=${room.id}">Xem sơ đồ phòng</a>
                        </div>
                    </section>

                    ${this.roomTypeSection()}

                    <section class="panel">
                        <div class="section-header">
                            <div><p class="eyebrow">Thông tin phòng</p><h2>Mô tả chi tiết</h2></div>
                        </div>
                        <p class="room-description">${escapeHtml(room.description || "Chủ trọ chưa cập nhật mô tả.")}</p>
                        <div class="room-facts">
                            ${fact("Tên nhà trọ", room.propertyName)}
                            ${fact("Diện tích phòng", `${displayValue(room.size)} m²`)}
                            ${fact("Sức chứa", `${displayValue(room.capacity)} người`)}
                            ${fact("Tổng số phòng", `${displayValue(room.totalRooms, 1)} phòng`)}
                            ${fact("Phòng còn trống", `${displayValue(room.availableRooms, 0)} phòng`)}
                            ${fact("Ngày có thể vào ở", formatDate(room.availableFrom))}
                            ${fact("Khu vực", room.areaName)}
                            ${fact("Trạng thái", labelRoomStatus(room.status))}
                        </div>
                    </section>

                    <section class="panel">
                        <div class="section-header">
                            <div><p class="eyebrow">Tiện nghi</p><h2>Trang bị tại phòng</h2></div>
                        </div>
                        <div class="room-amenity-grid">
                            ${(room.amenities || []).length
                                ? room.amenities.map(item => `<span class="room-amenity">${escapeHtml(item)}</span>`).join("")
                                : `<div class="empty-state">Chưa cập nhật tiện nghi.</div>`}
                        </div>
                    </section>

                    <section class="panel">
                        <div class="section-header">
                            <div><p class="eyebrow">Vị trí</p><h2>Địa chỉ phòng trọ</h2></div>
                            ${mapQuery ? `<a class="ghost-button link-button" href="${escapeHtml(googleMapsLink(mapQuery))}" target="_blank" rel="noopener">Mở Google Maps</a>` : ""}
                        </div>
                        <p>${escapeHtml(address)}</p>
                        ${mapQuery ? `<iframe class="room-detail-map" title="Bản đồ phòng trọ" loading="lazy" referrerpolicy="no-referrer-when-downgrade" src="${escapeHtml(googleMapsEmbed(mapQuery))}"></iframe>` : ""}
                    </section>

                    <section class="panel">
                        <div class="section-header">
                            <div><p class="eyebrow">Đánh giá</p><h2>Nhận xét gần đây</h2></div>
                        </div>
                        <div class="stack-list">
                            ${(room.surveys || []).length ? room.surveys.map(survey => `
                                <article class="stack-item room-review">
                                    <strong>${escapeHtml(survey.userName)}</strong>
                                    <div class="stack-meta">
                                        <span class="muted-badge">Vệ sinh ${survey.cleanlinessRating}/5</span>
                                        <span class="muted-badge">An ninh ${survey.securityRating}/5</span>
                                        <span class="muted-badge">Tiện nghi ${survey.convenienceRating}/5</span>
                                    </div>
                                    <p>${escapeHtml(survey.comment)}</p>
                                </article>`).join("") : `<div class="empty-state">Chưa có đánh giá nào.</div>`}
                        </div>
                        ${isTenant ? this.surveyForm(room.id) : ""}
                    </section>
                </div>

                <aside class="room-detail-sidebar">
                    <section class="panel owner-card">
                        <p class="eyebrow">Thông tin chủ trọ</p>
                        <div class="owner-avatar">${escapeHtml(initials(room.ownerName))}</div>
                        <h2>${escapeHtml(room.ownerName || "Chưa cập nhật")}</h2>
                        <p><strong>Số điện thoại:</strong> ${escapeHtml(room.contactPhone || room.ownerPhone || "Chưa cập nhật")}</p>
                        <p><strong>Địa chỉ trọ:</strong> ${escapeHtml(address)}</p>
                        <p><strong>Điều kiện hợp đồng:</strong> ${escapeHtml(room.contractNote || "Đang cập nhật")}</p>
                        ${isTenant ? `
                            <div class="card-actions">
                                <button class="primary-button" type="button" data-action="toggle-favorite">${room.favorite ? "Bỏ yêu thích" : "Lưu phòng"}</button>
                                <button class="ghost-button" type="button" data-action="toggle-contact">Nhắn chủ trọ</button>
                            </div>
                            <form id="contactForm" class="stack-form hidden">
                                <textarea name="content" placeholder="Nội dung muốn trao đổi với chủ trọ..." required></textarea>
                                <button class="primary-button" type="submit">Bắt đầu trò chuyện</button>
                            </form>
                            ${canRequest ? this.rentalForm(room.id) : `<div class="empty-state">Phòng hiện không nhận yêu cầu thuê mới.</div>`}
                        ` : this.guestActions()}
                    </section>
                </aside>
            </div>`;
    }

    roomTypeSection() {
        const roomTypes = this.layout?.roomTypes || [];
        return `
            <section class="panel">
                <div class="section-header">
                    <div><p class="eyebrow">Loại phòng</p><h2>Các lựa chọn trong nhà trọ</h2></div>
                    <a class="ghost-button link-button" href="/room-layout.html?id=${this.roomId}">Xem sơ đồ</a>
                </div>
                ${roomTypes.length ? `
                    <div class="room-type-grid">
                        ${roomTypes.map(type => `
                            <article class="room-type-card">
                                <div class="room-type-card-head">
                                    <div>
                                        <strong>${escapeHtml(type.name)}</strong>
                                        <span>${displayValue(type.size)} m² · ${displayValue(type.capacity)} người</span>
                                    </div>
                                    ${type.price ? `<span class="price-chip">${formatMoney(type.price)}/tháng</span>` : ""}
                                </div>
                                <div class="tag-row">
                                    <span class="muted-badge">${displayValue(type.availableRooms, 0)}/${displayValue(type.totalRooms, 0)} phòng trống</span>
                                    ${(type.amenities || []).slice(0, 4).map(item => `<span class="muted-badge">${escapeHtml(item)}</span>`).join("")}
                                </div>
                                ${type.description ? `<p>${escapeHtml(type.description)}</p>` : ""}
                            </article>`).join("")}
                    </div>` : `
                    <div class="empty-state">
                        Chủ trọ chưa cập nhật loại phòng. Bạn vẫn có thể xem sơ đồ phòng vật lý để chọn phòng còn trống.
                    </div>`}
            </section>`;
    }

    rentalForm(roomId) {
        return `
            <form id="rentalForm" class="stack-form detail-block" data-room-id="${roomId}">
                <h3>Gửi yêu cầu thuê</h3>
                <label class="field-group"><span class="field-label">Ngày dự kiến vào ở</span><input name="moveInDate" type="date" required></label>
                <textarea name="note" placeholder="Mô tả nhu cầu và thời hạn thuê..." required></textarea>
                <button class="primary-button" type="submit">Gửi yêu cầu</button>
            </form>`;
    }

    surveyForm(roomId) {
        return `
            <form id="surveyForm" class="stack-form detail-block" data-room-id="${roomId}">
                <h3>Viết đánh giá</h3>
                <div class="split-two">
                    <input name="cleanlinessRating" type="number" min="1" max="5" placeholder="Vệ sinh (1-5)" required>
                    <input name="securityRating" type="number" min="1" max="5" placeholder="An ninh (1-5)" required>
                </div>
                <input name="convenienceRating" type="number" min="1" max="5" placeholder="Tiện nghi (1-5)" required>
                <textarea name="comment" placeholder="Cảm nhận của bạn..." required></textarea>
                <button class="ghost-button" type="submit">Gửi đánh giá</button>
            </form>`;
    }

    guestActions() {
        if (this.user) return "";
        const returnUrl = encodeURIComponent(window.location.pathname + window.location.search);
        return `<a class="primary-button link-button" href="/auth.html?returnUrl=${returnUrl}">Đăng nhập để liên hệ</a>`;
    }

    async onClick(event) {
        const button = event.target.closest("[data-action]");
        if (!button) return;
        const action = button.dataset.action;
        try {
            if (action === "open-image") {
                openImageLightbox(button.dataset.url, this.room?.title || "Ảnh phòng trọ");
                return;
            }
            if (action === "close-image") {
                closeImageLightbox();
                return;
            }
            if (action === "select-image") {
                const image = document.querySelector(".room-detail-main-image img");
                const mainButton = document.querySelector(".room-detail-main-image [data-action='open-image']");
                const nextUrl = imageSrc(button.dataset.url);
                if (image) image.src = nextUrl;
                if (mainButton) mainButton.dataset.url = nextUrl;
            }
            if (action === "toggle-contact") {
                document.getElementById("contactForm")?.classList.toggle("hidden");
            }
            if (action === "toggle-favorite") {
                await this.api(`/api/rooms/${this.roomId}/favorite`, { method: "POST" });
                await this.loadRoom();
                this.showToast("Đã cập nhật danh sách yêu thích.");
            }
        } catch (error) {
            this.showToast(error.message, true);
        }
    }

    async onSubmit(event) {
        const form = event.target;
        if (!["rentalForm", "surveyForm", "contactForm"].includes(form.id)) return;
        event.preventDefault();
        const payload = Object.fromEntries(new FormData(form).entries());
        try {
            if (form.id === "rentalForm") {
                await this.api(`/api/interactions/rooms/${this.roomId}/rental-requests`, jsonOptions(payload));
                this.showToast("Đã gửi yêu cầu thuê.");
            }
            if (form.id === "surveyForm") {
                payload.cleanlinessRating = Number(payload.cleanlinessRating);
                payload.securityRating = Number(payload.securityRating);
                payload.convenienceRating = Number(payload.convenienceRating);
                await this.api(`/api/interactions/rooms/${this.roomId}/survey`, jsonOptions(payload));
                this.showToast("Đã gửi đánh giá.");
                await this.loadRoom();
            }
            if (form.id === "contactForm") {
                payload.roomId = Number(this.roomId);
                await this.api("/api/interactions/conversations", jsonOptions(payload));
                this.showToast("Đã tạo cuộc trò chuyện.");
                form.classList.add("hidden");
            }
            form.reset();
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
        this.detail.innerHTML = `<section class="panel"><div class="empty-state">${escapeHtml(message)} <a href="/#roomSection">Quay lại danh sách phòng</a></div></section>`;
    }

    showToast(message, isError = false) {
        this.toast.textContent = message;
        this.toast.style.background = isError ? "#cfcfcf" : "#ffffff";
        this.toast.style.color = "#111111";
        this.toast.classList.remove("hidden");
        clearTimeout(this.toastTimer);
        this.toastTimer = setTimeout(() => this.toast.classList.add("hidden"), 3200);
    }
}

function jsonOptions(body) {
    return { method: "POST", body: JSON.stringify(body) };
}

function fact(label, value) {
    return `<div class="room-fact"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value || "Chưa cập nhật")}</strong></div>`;
}

function uniqueImages(images) {
    const values = images.filter(Boolean);
    return [...new Set(values.length ? values : ["/room-placeholder.svg"])];
}

function displayValue(value, fallback = "Đang cập nhật") {
    return value === null || value === undefined || value === "" ? fallback : String(value);
}

function displayAddress(room) {
    const address = normalizeMapValue(room?.address);
    if (!isMapsUrl(address)) return address || room?.areaName || "Chưa cập nhật";
    return normalizeMapValue(room?.areaName) || "Xem vị trí trên Google Maps";
}

function googleMapsLink(query) {
    if (/^https?:\/\//i.test(query)) return query;
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
}

function googleMapsEmbed(query) {
    const coordinates = extractMapCoordinates(query);
    const value = coordinates ? `${coordinates.lat},${coordinates.lng}` : query;
    return `https://www.google.com/maps?q=${encodeURIComponent(value)}&output=embed`;
}

function buildMapSearchQuery(room) {
    const address = normalizeMapValue(room?.address);
    const mapQuery = normalizeMapValue(room?.mapQuery);
    const areaName = normalizeMapValue(room?.areaName);

    if (address) return address;
    if (mapQuery) return mapQuery;
    if (areaName) return areaName.includes("Thái Nguyên") ? areaName : `${areaName}, Thái Nguyên`;
    return "";
}

function normalizeMapValue(value) {
    return String(value ?? "").trim();
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

function formatMoney(value) {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0));
}

function formatDate(value) {
    if (!value) return "Đang cập nhật";
    return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function labelRoomStatus(status) {
    return {
        AVAILABLE: "Còn trống",
        OCCUPIED: "Đã có người thuê",
        MAINTENANCE: "Đang sửa chữa",
        EXPIRING_SOON: "Sắp hết hợp đồng"
    }[status] || status || "Đang cập nhật";
}

function initials(name) {
    return String(name || "CT").split(/\s+/).filter(Boolean).slice(-2).map(part => part[0]).join("").toUpperCase();
}

function imageSrc(value) {
    return String(value || "").trim() || "/room-placeholder.svg";
}

function imageFallback() {
    return "this.onerror=null;this.src='/room-placeholder.svg';";
}

function openImageLightbox(url, title = "Ảnh phòng trọ") {
    const src = imageSrc(url);
    let lightbox = document.getElementById("imageLightbox");
    if (!lightbox) {
        lightbox = document.createElement("div");
        lightbox.id = "imageLightbox";
        lightbox.className = "image-lightbox hidden";
        lightbox.innerHTML = `
            <button class="image-lightbox-backdrop" type="button" data-action="close-image" aria-label="Đóng ảnh"></button>
            <div class="image-lightbox-content" role="dialog" aria-modal="true" aria-label="Xem ảnh lớn">
                <button class="image-lightbox-close" type="button" data-action="close-image" aria-label="Đóng ảnh">×</button>
                <img alt="">
                <p></p>
            </div>`;
        document.body.appendChild(lightbox);
    }
    const image = lightbox.querySelector("img");
    const caption = lightbox.querySelector("p");
    image.src = src;
    image.alt = title;
    caption.textContent = "Bấm ESC hoặc vùng tối để đóng";
    lightbox.classList.remove("hidden");
    document.body.classList.add("image-lightbox-open");
}

function closeImageLightbox() {
    const lightbox = document.getElementById("imageLightbox");
    if (!lightbox) return;
    lightbox.classList.add("hidden");
    document.body.classList.remove("image-lightbox-open");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

document.addEventListener("DOMContentLoaded", () => new RoomDetailPage());
