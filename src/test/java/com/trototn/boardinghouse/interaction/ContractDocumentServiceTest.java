package com.trototn.boardinghouse.interaction;

import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.interaction.domain.RentalContract;
import com.trototn.boardinghouse.room.domain.PhysicalRoom;
import com.trototn.boardinghouse.room.domain.Room;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractDocumentServiceTest {
    @Test
    void fillsContractTemplateWithParticipantAndRoomData() throws Exception {
        RentalContract contract = contract();
        byte[] content = new ContractDocumentService().createDocument(contract);

        String text;
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            text = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (left, right) -> left + "\n" + right);
        }

        assertTrue(text.contains("Nguyễn Văn Chủ"));
        assertTrue(text.contains("Trần Thị Thuê"));
        assertTrue(text.contains("Phòng 101"));
        assertTrue(text.contains("Phòng 203"));
        assertTrue(text.contains("1.500.000"));
        assertFalse(text.contains("{{"), text);
    }

    private RentalContract contract() {
        User landlord = user("Nguyễn Văn Chủ", "012345678901", "Thái Nguyên", "0901000000",
                LocalDate.of(1980, 1, 2));
        User tenant = user("Trần Thị Thuê", "012345678902", "Bắc Ninh", "0902000000",
                LocalDate.of(2002, 3, 4));
        Room room = new Room();
        room.setTitle("Phòng 101");
        room.setAddress("Số 8, Tân Thịnh, Thái Nguyên");
        room.setSize(22.0);
        room.setCapacity(2);

        RentalContract contract = new RentalContract();
        contract.setLandlord(landlord);
        contract.setTenant(tenant);
        contract.setRoom(room);
        PhysicalRoom physicalRoom = new PhysicalRoom();
        physicalRoom.setRoomNumber("203");
        contract.setPhysicalRoom(physicalRoom);
        contract.setTenantCccd(tenant.getCccd());
        contract.setTenantAddress(tenant.getAddress());
        contract.setStartDate(LocalDate.of(2026, 7, 1));
        contract.setEndDate(LocalDate.of(2027, 7, 1));
        contract.setRent(1_500_000L);
        contract.setDeposit(1_500_000L);
        contract.setPaymentCycle("Ngày 05 hàng tháng");
        return contract;
    }

    private User user(String name, String cccd, String address, String phone, LocalDate dateOfBirth) {
        User user = new User();
        user.setFullName(name);
        user.setCccd(cccd);
        user.setAddress(address);
        user.setPhone(phone);
        user.setDateOfBirth(dateOfBirth);
        return user;
    }
}
