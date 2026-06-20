package com.trototn.boardinghouse.chatbot;

import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.RoomStatus;
import com.trototn.boardinghouse.room.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ChatbotAiClient chatbotAiClient;

    @Test
    void parsesPlainNumberPriceRangeAndFiltersByRoomRange() {
        when(roomRepository.findAll()).thenReturn(List.of(
                room(1L, "Trong khoảng", 700_000L, 1_200_000L),
                room(2L, "Cũng trong khoảng", 800_000L, 1_000_000L),
                room(3L, "Rẻ hơn khoảng", 450_000L, 650_000L),
                room(4L, "Cao hơn khoảng", 1_300_000L, 1_600_000L)
        ));
        when(chatbotAiClient.generateReply(anyString(), anyString(), anyList())).thenReturn(Optional.empty());

        ChatbotService service = new ChatbotService(roomRepository, chatbotAiClient);
        Responses.ChatbotReply reply = service.reply("tìm phòng có giá từ 700000 tới 1200000");

        assertEquals(700_000L, reply.minBudget());
        assertEquals(1_200_000L, reply.maxBudget());
        assertTrue(reply.reply().contains("700.000"));
        assertTrue(reply.reply().contains("1.200.000"));
        assertEquals(List.of(1L, 2L), reply.suggestions().stream().map(Responses.RoomView::id).toList());
    }

    @Test
    void analyzesSelectedRoomStrengthsWhenPromptReferencesThisRoom() {
        Room room = room(10L, "Phong gan truong", 800_000L, 1_000_000L);
        room.setAmenities(List.of("wifi", "ban cong"));
        room.setDescription("Phong sach, thoang, phu hop sinh vien di hoc gan truong.");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(chatbotAiClient.generateReply(anyString(), anyString(), anyList())).thenReturn(Optional.empty());

        ChatbotService service = new ChatbotService(roomRepository, chatbotAiClient);
        Responses.ChatbotReply reply = service.reply("phong nay co uu diem gi", 10L);

        assertTrue(reply.reply().contains("Phan tich nhanh") || reply.reply().contains("Phân tích nhanh"));
        assertTrue(reply.reply().contains("800.000"));
        assertTrue(reply.reply().contains("wifi"));
        assertEquals(List.of(10L), reply.suggestions().stream().map(Responses.RoomView::id).toList());
    }

    @Test
    void analyzesRoomMentionedByTitleWithoutSelectedRoomId() {
        Room target = room(20L, "phong tro gia hat re", 900_000L, 1_200_000L);
        target.setAmenities(List.of("giuong", "ban"));
        Room other = room(21L, "phong moi sach dep", 700_000L, 900_000L);
        when(roomRepository.findAll()).thenReturn(List.of(other, target));
        when(chatbotAiClient.generateReply(anyString(), anyString(), anyList())).thenReturn(Optional.empty());

        ChatbotService service = new ChatbotService(roomRepository, chatbotAiClient);
        Responses.ChatbotReply reply = service.reply("phong tro gia hat re co uu diem gi");

        assertTrue(reply.reply().contains("nhanh"));
        assertTrue(reply.reply().contains("900.000"));
        assertTrue(reply.reply().contains("giuong"));
        assertEquals(List.of(20L), reply.suggestions().stream().map(Responses.RoomView::id).toList());
    }

    private Room room(Long id, String title, Long minPrice, Long maxPrice) {
        Room room = new Room();
        room.setId(id);
        room.setTitle(title);
        room.setPropertyName("Nhà trọ test");
        room.setAreaName("Gần ĐH CNTT");
        room.setAddress("Thái Nguyên");
        room.setPrice(minPrice);
        room.setMinPrice(minPrice);
        room.setMaxPrice(maxPrice);
        room.setSize(15.0);
        room.setCapacity(2);
        room.setTotalRooms(10);
        room.setAvailableRooms(5);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setModerationStatus(ModerationStatus.APPROVED);
        return room;
    }
}
