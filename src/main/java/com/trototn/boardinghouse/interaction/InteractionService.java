package com.trototn.boardinghouse.interaction;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.common.MapperUtil;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.interaction.domain.Conversation;
import com.trototn.boardinghouse.interaction.domain.Favorite;
import com.trototn.boardinghouse.interaction.domain.Message;
import com.trototn.boardinghouse.interaction.domain.RentalRequest;
import com.trototn.boardinghouse.interaction.domain.RentalStatus;
import com.trototn.boardinghouse.interaction.domain.Survey;
import com.trototn.boardinghouse.interaction.repository.ConversationRepository;
import com.trototn.boardinghouse.interaction.repository.FavoriteRepository;
import com.trototn.boardinghouse.interaction.repository.MessageRepository;
import com.trototn.boardinghouse.interaction.repository.RentalRequestRepository;
import com.trototn.boardinghouse.interaction.repository.SurveyRepository;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.RoomStatus;
import com.trototn.boardinghouse.room.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InteractionService {
    private final FavoriteRepository favoriteRepository;
    private final RoomRepository roomRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SurveyRepository surveyRepository;
    private final RealtimeEventService realtimeEventService;

    public InteractionService(FavoriteRepository favoriteRepository, RoomRepository roomRepository,
                               RentalRequestRepository rentalRequestRepository,
                               ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               SurveyRepository surveyRepository,
                               RealtimeEventService realtimeEventService) {
        this.favoriteRepository = favoriteRepository;
        this.roomRepository = roomRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.surveyRepository = surveyRepository;
        this.realtimeEventService = realtimeEventService;
    }

    @Transactional
    public void toggleFavorite(User tenant, Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        favoriteRepository.findByTenantAndRoom(tenant, room).ifPresentOrElse(
                favoriteRepository::delete,
                () -> {
                    Favorite fav = new Favorite();
                    fav.setTenant(tenant);
                    fav.setRoom(room);
                    favoriteRepository.save(fav);
                }
        );
    }

    public List<Responses.RoomView> listFavorites(User tenant) {
        List<Long> ids = favoriteRepository.findByTenantId(tenant.getId()).stream()
                .map(f -> f.getRoom().getId()).toList();
        List<Room> rooms = roomRepository.findAllById(ids);
        return rooms.stream()
                .map(r -> MapperUtil.toRoomView(r, true, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Responses.RentalRequestView> listRentalRequests(User user) {
        List<RentalRequest> requests;
        if (user.getRole() == Role.LANDLORD) {
            requests = rentalRequestRepository.findByLandlordId(user.getId());
        } else if (user.getRole() == Role.ADMIN) {
            requests = rentalRequestRepository.findAll();
        } else {
            requests = rentalRequestRepository.findByTenantId(user.getId());
        }
        return requests.stream().map(MapperUtil::toRentalView).collect(Collectors.toList());
    }

    @Transactional
    public Responses.RentalRequestView createRentalRequest(User tenant, Long roomId, LocalDate moveInDate, String note) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (room.getOwner().getId().equals(tenant.getId())) {
            throw new IllegalArgumentException("Bạn không thể gửi yêu cầu cho phòng của chính mình");
        }
        if (room.getModerationStatus() != ModerationStatus.APPROVED) {
            throw new IllegalArgumentException("Phòng chưa sẵn sàng hiển thị");
        }
        if (!(room.getStatus() == RoomStatus.AVAILABLE || room.getStatus() == RoomStatus.EXPIRING_SOON)) {
            throw new IllegalArgumentException("Phòng hiện không nhận yêu cầu mới");
        }
        RentalRequest r = new RentalRequest();
        r.setRoom(room);
        r.setTenant(tenant);
        r.setLandlord(room.getOwner());
        r.setMoveInDate(moveInDate);
        r.setNote(note);
        r.setStatus(RentalStatus.PENDING);
        r.setUpdatedAt(Instant.now());
        rentalRequestRepository.save(r);
        room.setContactCount(Optional.ofNullable(room.getContactCount()).orElse(0L) + 1);
        return MapperUtil.toRentalView(r);
    }

    @Transactional
    public void updateRentalStatus(User actor, Long requestId, RentalStatus status) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        boolean landlordActor = actor.getRole() == Role.LANDLORD && request.getLandlord().getId().equals(actor.getId());
        boolean adminActor = actor.getRole() == Role.ADMIN;
        boolean tenantActor = actor.getRole() == Role.TENANT && request.getTenant().getId().equals(actor.getId()) && status == RentalStatus.CANCELLED;
        if (!(landlordActor || adminActor || tenantActor)) {
            throw new IllegalArgumentException("Not allowed");
        }
        request.setStatus(status);
        request.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<Responses.ConversationView> listConversations(User user) {
        List<Conversation> conversations = conversationRepository.findByTenantIdOrLandlordId(user.getId(), user.getId());
        return conversations.stream()
                .map(c -> {
                    List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
                    return MapperUtil.toConversationView(c, messages);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Responses.ConversationView getConversation(User user, Long id) {
        Conversation c = conversationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        if (!c.getTenant().getId().equals(user.getId()) && !c.getLandlord().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Not allowed");
        }
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
        return MapperUtil.toConversationView(c, messages);
    }

    @Transactional
    public Responses.ConversationView createConversation(User tenant, Long roomId, String firstMessage) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (room.getModerationStatus() != ModerationStatus.APPROVED) {
            throw new IllegalArgumentException("Phòng chưa sẵn sàng hiển thị");
        }
        Conversation c = conversationRepository
                .findByTenantIdAndLandlordIdAndRoomId(tenant.getId(), room.getOwner().getId(), room.getId())
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setRoom(room);
                    created.setTenant(tenant);
                    created.setLandlord(room.getOwner());
                    return conversationRepository.save(created);
                });
        if (firstMessage != null && !firstMessage.isBlank()) {
            Message m = new Message();
            m.setConversation(c);
            m.setSender(tenant);
            m.setContent(firstMessage);
            messageRepository.save(m);
            c.setUpdatedAt(Instant.now());
            realtimeEventService.publishNewMessage(c.getId(), tenant.getFullName(), c.getTenant().getId(), c.getLandlord().getId());
        }
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
        return MapperUtil.toConversationView(c, messages);
    }

    @Transactional
    public Responses.ConversationView sendMessage(User sender, Long conversationId, String content) {
        Conversation c = conversationRepository.findById(conversationId).orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        boolean allowed = sender.getRole() == Role.ADMIN ||
                c.getTenant().getId().equals(sender.getId()) ||
                c.getLandlord().getId().equals(sender.getId());
        if (!allowed) throw new IllegalArgumentException("Not allowed");
        Message m = new Message();
        m.setConversation(c);
        m.setSender(sender);
        m.setContent(content);
        messageRepository.save(m);
        c.setUpdatedAt(Instant.now());
        realtimeEventService.publishNewMessage(c.getId(), sender.getFullName(), c.getTenant().getId(), c.getLandlord().getId());
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
        return MapperUtil.toConversationView(c, messages);
    }

    @Transactional
    public Responses.RoomView addSurvey(User user, Long roomId, int clean, int security, int convenience, String comment) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (clean < 1 || clean > 5 || security < 1 || security > 5 || convenience < 1 || convenience > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nhận xét");
        }
        boolean hasHistory = rentalRequestRepository.findByTenantIdAndRoomId(user.getId(), roomId).stream()
                .anyMatch(r -> r.getStatus() == RentalStatus.APPROVED);
        if (!hasHistory) {
            throw new IllegalArgumentException("Bạn cần có giao dịch thành công với phòng này trước khi đánh giá");
        }
        boolean already = surveyRepository.findByRoomIdAndUserId(roomId, user.getId()).stream().findFirst().isPresent();
        if (already) {
            throw new IllegalArgumentException("Bạn đã gửi đánh giá cho phòng này");
        }
        Survey s = new Survey();
        s.setRoom(room);
        s.setUser(user);
        s.setCleanlinessRating(clean);
        s.setSecurityRating(security);
        s.setConvenienceRating(convenience);
        s.setComment(comment);
        surveyRepository.save(s);
        updateSurveyStats(room);
        List<Survey> surveys = surveyRepository.findByRoomId(room.getId());
        boolean favorite = favoriteRepository.findByTenantAndRoom(user, room).isPresent();
        return MapperUtil.toRoomView(room, favorite, surveys);
    }

    private void updateSurveyStats(Room room) {
        List<Survey> surveys = surveyRepository.findByRoomId(room.getId());
        if (surveys.isEmpty()) {
            room.setSurveyAverage(0.0);
            room.setSurveyCount(0);
            return;
        }
        double avg = surveys.stream()
                .mapToDouble(s -> (s.getCleanlinessRating() + s.getSecurityRating() + s.getConvenienceRating()) / 3.0)
                .average()
                .orElse(0.0);
        room.setSurveyAverage(Math.round(avg * 10.0) / 10.0);
        room.setSurveyCount(surveys.size());
    }
}
