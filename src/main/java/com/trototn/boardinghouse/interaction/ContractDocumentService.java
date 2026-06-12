package com.trototn.boardinghouse.interaction;

import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.interaction.domain.RentalContract;
import com.trototn.boardinghouse.room.domain.Room;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ContractDocumentService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));

    public byte[] createDocument(RentalContract contract) {
        try (InputStream template = new ClassPathResource(
                "templates/hop-dong-thue-nha-tro.docx").getInputStream();
             XWPFDocument document = new XWPFDocument(template);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Map<String, String> values = contractValues(contract);
            document.getParagraphs().forEach(paragraph -> replaceParagraph(paragraph, values));
            for (XWPFTable table : document.getTables()) {
                table.getRows().forEach(row -> row.getTableCells().forEach(cell -> replaceCell(cell, values)));
            }
            document.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể tạo file hợp đồng", ex);
        }
    }

    private Map<String, String> contractValues(RentalContract contract) {
        User landlord = contract.getLandlord();
        User tenant = contract.getTenant();
        Room room = contract.getRoom();
        int durationMonths = Math.max(1,
                Period.between(contract.getStartDate(), contract.getEndDate()).getYears() * 12
                        + Period.between(contract.getStartDate(), contract.getEndDate()).getMonths());

        Map<String, String> values = new LinkedHashMap<>();
        values.put("{{CONTRACT_DATE_TEXT}}", vietnameseDate(LocalDate.now()));
        values.put("{{CONTRACT_LOCATION}}", value(room.getAddress()));
        values.put("{{LANDLORD_NAME}}", value(landlord.getFullName()));
        values.put("{{LANDLORD_CCCD}}", value(landlord.getCccd()));
        values.put("{{LANDLORD_DOB}}", date(landlord.getDateOfBirth()));
        values.put("{{LANDLORD_ADDRESS}}", value(landlord.getAddress()));
        values.put("{{LANDLORD_PHONE}}", value(landlord.getPhone()));
        values.put("{{TENANT_NAME}}", value(tenant.getFullName()));
        values.put("{{TENANT_CCCD}}", value(contract.getTenantCccd()));
        values.put("{{TENANT_DOB}}", date(tenant.getDateOfBirth()));
        values.put("{{TENANT_ADDRESS}}", value(contract.getTenantAddress()));
        values.put("{{TENANT_PHONE}}", value(tenant.getPhone()));
        String roomTitle = contract.getPhysicalRoom() == null
                ? room.getTitle()
                : room.getTitle() + " - Phòng " + contract.getPhysicalRoom().getRoomNumber();
        values.put("{{ROOM_TITLE}}", value(roomTitle));
        values.put("{{ROOM_ADDRESS}}", value(room.getAddress()));
        values.put("{{ROOM_SIZE}}", room.getSize() == null ? "Chưa cập nhật" : trimNumber(room.getSize()));
        values.put("{{ROOM_CAPACITY}}", room.getCapacity() == null ? "Chưa cập nhật" : room.getCapacity().toString());
        values.put("{{DURATION_MONTHS}}", Integer.toString(durationMonths));
        values.put("{{START_DATE}}", date(contract.getStartDate()));
        values.put("{{END_DATE}}", date(contract.getEndDate()));
        values.put("{{RENT}}", NUMBER_FORMAT.format(contract.getRent()));
        values.put("{{RENT_WORDS}}", VietnameseNumberWords.toWords(contract.getRent()));
        values.put("{{DEPOSIT}}", NUMBER_FORMAT.format(contract.getDeposit()));
        values.put("{{DEPOSIT_WORDS}}", VietnameseNumberWords.toWords(contract.getDeposit()));
        values.put("{{PAYMENT_CYCLE}}", value(contract.getPaymentCycle()));
        return values;
    }

    private void replaceCell(XWPFTableCell cell, Map<String, String> values) {
        cell.getParagraphs().forEach(paragraph -> replaceParagraph(paragraph, values));
        cell.getTables().forEach(table -> table.getRows().forEach(
                row -> row.getTableCells().forEach(nested -> replaceCell(nested, values))));
    }

    private void replaceParagraph(XWPFParagraph paragraph, Map<String, String> values) {
        String original = paragraph.getText();
        String replaced = replace(original, values);
        if (original.equals(replaced)) {
            return;
        }
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        XWPFRun target = paragraph.createRun();
        target.setText(replaced);
    }

    private String replace(String text, Map<String, String> values) {
        String result = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "Chưa cập nhật" : value.trim();
    }

    private String date(LocalDate value) {
        return value == null ? "Chưa cập nhật" : DATE_FORMAT.format(value);
    }

    private String vietnameseDate(LocalDate value) {
        return "ngày " + value.getDayOfMonth() + " tháng " + value.getMonthValue() + " năm " + value.getYear();
    }

    private String trimNumber(Double value) {
        return value % 1 == 0 ? Long.toString(value.longValue()) : value.toString();
    }
}
