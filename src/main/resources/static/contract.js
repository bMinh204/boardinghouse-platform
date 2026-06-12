document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("contractForm");
    const toast = document.getElementById("toast");
    const requestId = new URLSearchParams(window.location.search).get("requestId");

    if (!form || !requestId || !/^\d+$/.test(requestId)) {
        showError("Không tìm thấy yêu cầu thuê cần lập hợp đồng.");
        return;
    }

    loadDraft();

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const payload = Object.fromEntries(new FormData(form).entries());
        payload.status = "APPROVED";
        payload.deposit = Number(payload.deposit);
        payload.rent = Number(payload.rent);
        try {
            await api(`/api/interactions/rental-requests/${requestId}`, {
                method: "PATCH",
                body: JSON.stringify(payload)
            });
            showToast("Đã duyệt yêu cầu và tạo hợp đồng.");
            window.location.href = `/api/interactions/rental-requests/${requestId}/contract`;
            setTimeout(() => {
                window.location.href = "/index.html#landlord";
            }, 1200);
        } catch (error) {
            showToast(error.message, true);
        }
    });

    async function loadDraft() {
        try {
            const data = await api(`/api/interactions/rental-requests/${requestId}/contract-draft`);
            const contract = data.contract;
            renderFacts("landlordInfo", [
                ["Họ tên", contract.landlordName],
                ["Ngày sinh", formatDate(contract.landlordDateOfBirth)],
                ["CCCD", contract.landlordCccd],
                ["Địa chỉ", contract.landlordAddress],
                ["Điện thoại", contract.landlordPhone]
            ]);
            renderFacts("tenantInfo", [
                ["Họ tên", contract.tenantName],
                ["Ngày sinh", formatDate(contract.tenantDateOfBirth)],
                ["CCCD", contract.tenantCccd],
                ["Địa chỉ", contract.tenantAddress],
                ["Điện thoại", contract.tenantPhone]
            ]);
            renderFacts("roomInfo", [
                ["Tên phòng", contract.roomTitle],
                ["Địa chỉ", contract.roomAddress],
                ["Diện tích", `${contract.roomSize ?? "Chưa cập nhật"} m²`],
                ["Sức chứa", `${contract.roomCapacity ?? "Chưa cập nhật"} người`]
            ]);
            form.rent.value = contract.suggestedRent ?? "";
            form.startDate.value = contract.suggestedStartDate ?? "";
            form.tenantCccd.value = contract.tenantCccd ?? "";
            form.tenantAddress.value = contract.tenantAddress ?? "";
            if (contract.suggestedStartDate) {
                const endDate = new Date(`${contract.suggestedStartDate}T00:00:00`);
                endDate.setFullYear(endDate.getFullYear() + 1);
                form.endDate.value = endDate.toISOString().slice(0, 10);
            }
            document.getElementById("contractLoading").classList.add("hidden");
            document.getElementById("contractAutoInfo").classList.remove("hidden");
            form.classList.remove("hidden");
        } catch (error) {
            showError(error.message);
        }
    }

    async function api(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            headers: { "Content-Type": "application/json" },
            ...options
        });
        let data = {};
        try { data = await response.json(); } catch (error) { data = {}; }
        if (!response.ok) throw new Error(data.message || "Yêu cầu thất bại.");
        return data;
    }

    function renderFacts(id, rows) {
        document.getElementById(id).innerHTML = rows.map(([label, value]) => `
            <div class="room-fact">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(value || "Chưa cập nhật")}</strong>
            </div>`).join("");
    }

    function showError(message) {
        document.getElementById("contractLoading")?.classList.add("hidden");
        const error = document.getElementById("contractError");
        error.textContent = message;
        error.classList.remove("hidden");
    }

    function showToast(message, isError = false) {
        if (!toast) return;
        toast.textContent = message;
        toast.style.background = isError ? "#cfcfcf" : "#ffffff";
        toast.style.color = "#111111";
        toast.classList.remove("hidden");
        setTimeout(() => toast.classList.add("hidden"), 3200);
    }

    function formatDate(value) {
        if (!value) return "Chưa cập nhật";
        return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium" })
            .format(new Date(`${value}T00:00:00`));
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }
});
