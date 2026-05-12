package com.trototn.boardinghouse.dashboard;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.common.MapperUtil;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.interaction.domain.RentalRequest;
import com.trototn.boardinghouse.interaction.repository.ConversationRepository;
import com.trototn.boardinghouse.interaction.repository.MessageRepository;
import com.trototn.boardinghouse.interaction.repository.RentalRequestRepository;
import com.trototn.boardinghouse.interaction.repository.SurveyRepository;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.RoomStatus;
import com.trototn.boardinghouse.room.repository.RoomRepository;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final RoomRepository roomRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;

    public DashboardService(RoomRepository roomRepository,
                            RentalRequestRepository rentalRequestRepository,
                            ConversationRepository conversationRepository,
                            MessageRepository messageRepository,
                            UserRepository userRepository,
                            SurveyRepository surveyRepository) {
        this.roomRepository = roomRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.surveyRepository = surveyRepository;
    }

    public Responses.LandlordDashboard landlordDashboard(User landlord) {
        List<Room> rooms = roomRepository.findByOwner(landlord);
        long totalRooms = rooms.size();
        long available = rooms.stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
        long occupied = rooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        long maintenance = rooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();
        long expiring = rooms.stream().filter(r -> r.getStatus() == RoomStatus.EXPIRING_SOON).count();
        long pending = rooms.stream().filter(r -> r.getModerationStatus() == ModerationStatus.PENDING).count();
        long views = rooms.stream().mapToLong(r -> Optional.ofNullable(r.getViewCount()).orElse(0L)).sum();
        long contacts = rooms.stream().mapToLong(r -> Optional.ofNullable(r.getContactCount()).orElse(0L)).sum();
        return new Responses.LandlordDashboard(totalRooms, available, occupied, maintenance, expiring, views, contacts, pending);
    }

    @Transactional(readOnly = true)
    public Responses.AdminDashboard adminDashboard() {
        List<Room> rooms = roomRepository.findAll();
        long approvedRooms = rooms.stream().filter(r -> r.getModerationStatus() == ModerationStatus.APPROVED).count();
        long pendingRooms = rooms.stream().filter(r -> r.getModerationStatus() == ModerationStatus.PENDING).count();
        long occupiedRooms = rooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        long totalViews = rooms.stream().mapToLong(r -> Optional.ofNullable(r.getViewCount()).orElse(0L)).sum();
        long totalConversations = conversationRepository.count();
        long totalUsers = userRepository.count();
        long tenantCount = userRepository.countByRole(Role.TENANT);
        long landlordCount = userRepository.countByRole(Role.LANDLORD);

        List<Responses.ReportRentalItem> recentRental = rentalRequestRepository.findAll().stream()
                .sorted(Comparator.comparing(RentalRequest::getUpdatedAt).reversed())
                .limit(10)
                .map(r -> new Responses.ReportRentalItem(
                        r.getRoom().getTitle(),
                        r.getTenant().getFullName(),
                        r.getLandlord().getFullName(),
                        r.getStatus(),
                        r.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        List<Responses.ReportItem> monthly = aggregateByMonth();
        List<Responses.ReportItem> quarterly = aggregateByQuarter();
        List<Responses.ReportItem> yearly = aggregateByYear();

        return new Responses.AdminDashboard(totalUsers, tenantCount, landlordCount, approvedRooms, pendingRooms,
                occupiedRooms, totalViews, totalConversations, recentRental, monthly, quarterly, yearly);
    }

    private List<Responses.ReportItem> aggregateByMonth() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (RentalRequest r : rentalRequestRepository.findAll()) {
            Instant time = r.getUpdatedAt();
            var zdt = time.atZone(ZoneId.systemDefault());
            String key = zdt.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            map.put(key, map.getOrDefault(key, 0L) + 1);
        }
        return map.entrySet().stream()
                .map(e -> new Responses.ReportItem(e.getKey(), e.getValue()))
                .toList();
    }

    private List<Responses.ReportItem> aggregateByQuarter() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (RentalRequest r : rentalRequestRepository.findAll()) {
            var zdt = r.getUpdatedAt().atZone(ZoneId.systemDefault());
            int quarter = (zdt.getMonthValue() - 1) / 3 + 1;
            String key = "Q" + quarter;
            map.put(key, map.getOrDefault(key, 0L) + 1);
        }
        return map.entrySet().stream()
                .map(e -> new Responses.ReportItem(e.getKey(), e.getValue()))
                .toList();
    }

    private List<Responses.ReportItem> aggregateByYear() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (RentalRequest r : rentalRequestRepository.findAll()) {
            int year = r.getUpdatedAt().atZone(ZoneId.systemDefault()).getYear();
            map.put(String.valueOf(year), map.getOrDefault(String.valueOf(year), 0L) + 1);
        }
        return map.entrySet().stream()
                .map(e -> new Responses.ReportItem(e.getKey(), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> backup() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", userRepository.findAll().stream()
                .map(MapperUtil::toUserDetail)
                .toList());
        data.put("rooms", roomRepository.findAll().stream()
                .map(room -> MapperUtil.toRoomView(room, false, surveyRepository.findByRoomId(room.getId())))
                .toList());
        data.put("rentalRequests", rentalRequestRepository.findAll().stream()
                .map(MapperUtil::toRentalView)
                .toList());
        data.put("conversations", conversationRepository.findAll().stream()
                .map(conversation -> MapperUtil.toConversationView(
                        conversation,
                        messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())))
                .toList());
        data.put("surveys", surveyRepository.findAll().stream()
                .map(MapperUtil::toSurveyView)
                .toList());
        return data;
    }
}
