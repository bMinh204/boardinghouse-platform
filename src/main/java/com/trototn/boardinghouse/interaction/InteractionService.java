package com.trototn.boardinghouse.interaction;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.common.MapperUtil;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.interaction.domain.Conversation;
import com.trototn.boardinghouse.interaction.domain.Favorite;
import com.trototn.boardinghouse.interaction.domain.Message;
import com.trototn.boardinghouse.interaction.domain.RentalContract;
import com.trototn.boardinghouse.interaction.domain.RentalRequest;
import com.trototn.boardinghouse.interaction.domain.RentalStatus;
import com.trototn.boardinghouse.interaction.domain.ContractStatus;
import com.trototn.boardinghouse.interaction.domain.ResidenceStatus;
import com.trototn.boardinghouse.interaction.domain.ResidenceType;
import com.trototn.boardinghouse.interaction.domain.Survey;
import com.trototn.boardinghouse.interaction.domain.TemporaryResidence;
import com.trototn.boardinghouse.interaction.repository.ConversationRepository;
import com.trototn.boardinghouse.interaction.repository.FavoriteRepository;
import com.trototn.boardinghouse.interaction.repository.MessageRepository;
import com.trototn.boardinghouse.interaction.repository.RentalContractRepository;
import com.trototn.boardinghouse.interaction.repository.RentalRequestRepository;
import com.trototn.boardinghouse.interaction.repository.SurveyRepository;
import com.trototn.boardinghouse.interaction.repository.TemporaryResidenceRepository;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.RoomStatus;
import com.trototn.boardinghouse.room.domain.PhysicalRoom;
import com.trototn.boardinghouse.room.domain.PhysicalRoomStatus;
import com.trototn.boardinghouse.room.RoomLayoutService;
import com.trototn.boardinghouse.room.repository.PhysicalRoomRepository;
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
    private final RentalContractRepository rentalContractRepository;
    private final TemporaryResidenceRepository temporaryResidenceRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SurveyRepository surveyRepository;
    private final RealtimeEventService realtimeEventService;
    private final PhysicalRoomRepository physicalRoomRepository;
    private final RoomLayoutService roomLayoutService;

    public InteractionService(FavoriteRepository favoriteRepository, RoomRepository roomRepository,
                               RentalRequestRepository rentalRequestRepository,
                               RentalContractRepository rentalContractRepository,
                               TemporaryResidenceRepository temporaryResidenceRepository,
                               ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               SurveyRepository surveyRepository,
                               RealtimeEventService realtimeEventService,
                               PhysicalRoomRepository physicalRoomRepository,
                               RoomLayoutService roomLayoutService) {
        this.favoriteRepository = favoriteRepository;
        this.roomRepository = roomRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.temporaryResidenceRepository = temporaryResidenceRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.surveyRepository = surveyRepository;
        this.realtimeEventService = realtimeEventService;
        this.physicalRoomRepository = physicalRoomRepository;
        this.roomLayoutService = roomLayoutService;
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
        return requests.stream()
                .map(request -> MapperUtil.toRentalView(
                        request, rentalContractRepository.existsByRentalRequestId(request.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public Responses.RentalRequestView createRentalRequest(User tenant, Long roomId, LocalDate moveInDate, String note) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (physicalRoomRepository.countByRoomId(roomId) > 0) {
            throw new IllegalArgumentException("Vui lòng chọn phòng cụ thể trên sơ đồ nhà trọ");
        }
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
    public Responses.RentalRequestView holdPhysicalRoom(User tenant, Long roomId, Long physicalRoomId,
            LocalDate moveInDate, String note) {
        PhysicalRoom physicalRoom = physicalRoomRepository.findWithLockById(physicalRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));
        Room room = physicalRoom.getRoom();
        if (!room.getId().equals(roomId)) {
            throw new IllegalArgumentException("Phòng không thuộc nhà trọ này");
        }
        if (room.getOwner().getId().equals(tenant.getId())) {
            throw new IllegalArgumentException("Bạn không thể giữ phòng của chính mình");
        }
        if (room.getModerationStatus() != ModerationStatus.APPROVED) {
            throw new IllegalArgumentException("Nhà trọ chưa sẵn sàng nhận yêu cầu");
        }
        releaseExpiredHoldIfNeeded(physicalRoom);
        if (physicalRoom.getStatus() != PhysicalRoomStatus.AVAILABLE) {
            throw new IllegalArgumentException("Phòng này vừa được người khác chọn hoặc không còn trống");
        }
        if (rentalRequestRepository.existsByPhysicalRoomIdAndStatus(physicalRoomId, RentalStatus.PENDING)) {
            throw new IllegalArgumentException("Phòng đang có yêu cầu chờ xác nhận");
        }

        Instant expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
        physicalRoom.setStatus(PhysicalRoomStatus.HELD);
        physicalRoom.setHeldBy(tenant);
        physicalRoom.setHoldExpiresAt(expiresAt);

        RentalRequest request = new RentalRequest();
        request.setRoom(room);
        request.setPhysicalRoom(physicalRoom);
        request.setTenant(tenant);
        request.setLandlord(room.getOwner());
        request.setMoveInDate(moveInDate != null ? moveInDate : LocalDate.now());
        request.setNote(note == null || note.isBlank()
                ? "Yêu cầu giữ phòng " + physicalRoom.getRoomNumber() + " trong 24 giờ"
                : note.trim());
        request.setStatus(RentalStatus.PENDING);
        request.setExpiresAt(expiresAt);
        rentalRequestRepository.save(request);
        room.setContactCount(Optional.ofNullable(room.getContactCount()).orElse(0L) + 1);
        roomLayoutService.syncRoomCounts(roomId);
        return MapperUtil.toRentalView(request);
    }

    @Transactional
    public void cancelPhysicalRoomHold(User tenant, Long roomId, Long physicalRoomId) {
        RentalRequest request = rentalRequestRepository
                .findFirstByPhysicalRoomIdAndStatus(physicalRoomId, RentalStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu giữ phòng"));
        if (!request.getRoom().getId().equals(roomId)
                || !request.getTenant().getId().equals(tenant.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền hủy yêu cầu này");
        }
        releasePhysicalRoom(request);
        request.setStatus(RentalStatus.CANCELLED);
        request.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void updateRentalStatus(User actor, Long requestId, InteractionController.RentalStatusRequest payload) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        RentalStatus status = payload.status();
        boolean landlordActor = actor.getRole() == Role.LANDLORD && request.getLandlord().getId().equals(actor.getId());
        boolean adminActor = actor.getRole() == Role.ADMIN;
        boolean tenantActor = actor.getRole() == Role.TENANT && request.getTenant().getId().equals(actor.getId()) && status == RentalStatus.CANCELLED;
        if (!(landlordActor || adminActor || tenantActor)) {
            throw new IllegalArgumentException("Not allowed");
        }
        boolean approvedWithoutContract = request.getStatus() == RentalStatus.APPROVED
                && status == RentalStatus.APPROVED
                && !rentalContractRepository.existsByRentalRequestId(requestId);
        if (request.getStatus() != RentalStatus.PENDING && !approvedWithoutContract) {
            throw new IllegalArgumentException("Yêu cầu này đã được xử lý");
        }
        if (approvedWithoutContract) {
            if (!(landlordActor || adminActor)) {
                throw new IllegalArgumentException("Not allowed");
            }
            createContractIfNeeded(request, payload);
            request.setUpdatedAt(Instant.now());
            return;
        }
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(Instant.now())) {
            expireRequest(request);
            throw new IllegalArgumentException("Yêu cầu giữ phòng đã hết hạn");
        }
        if (status == RentalStatus.APPROVED) {
            if (!(landlordActor || adminActor)) {
                throw new IllegalArgumentException("Not allowed");
            }
            createContractIfNeeded(request, payload);
            if (request.getPhysicalRoom() != null) {
                PhysicalRoom physicalRoom = physicalRoomRepository.findWithLockById(request.getPhysicalRoom().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));
                if (physicalRoom.getStatus() != PhysicalRoomStatus.HELD
                        || physicalRoom.getHeldBy() == null
                        || !physicalRoom.getHeldBy().getId().equals(request.getTenant().getId())) {
                    throw new IllegalArgumentException("Phòng không còn được giữ bởi người thuê này");
                }
                physicalRoom.setStatus(PhysicalRoomStatus.OCCUPIED);
                clearHold(physicalRoom);
                roomLayoutService.syncRoomCounts(request.getRoom().getId());
            } else {
                Room room = request.getRoom();
                int availableRooms = room.getAvailableRooms() != null
                        ? room.getAvailableRooms()
                        : room.getStatus() == RoomStatus.OCCUPIED ? 0 : 1;
                int remainingRooms = Math.max(availableRooms - 1, 0);
                room.setAvailableRooms(remainingRooms);
                room.setStatus(remainingRooms > 0 ? RoomStatus.AVAILABLE : RoomStatus.OCCUPIED);
            }
        } else if ((status == RentalStatus.REJECTED || status == RentalStatus.CANCELLED)
                && request.getPhysicalRoom() != null) {
            releasePhysicalRoom(request);
        }
        request.setStatus(status);
        request.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public Responses.ContractDraft getContractDraft(User actor, Long requestId) {
        RentalRequest request = rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu thuê"));
        boolean landlordActor = actor.getRole() == Role.LANDLORD
                && request.getLandlord().getId().equals(actor.getId());
        boolean adminActor = actor.getRole() == Role.ADMIN;
        if (!(landlordActor || adminActor)) {
            throw new IllegalArgumentException("Bạn không có quyền lập hợp đồng này");
        }
        User landlord = request.getLandlord();
        User tenant = request.getTenant();
        Room room = request.getRoom();
        return new Responses.ContractDraft(
                request.getId(),
                landlord.getFullName(),
                landlord.getDateOfBirth(),
                landlord.getAddress(),
                landlord.getCccd(),
                landlord.getPhone(),
                tenant.getFullName(),
                tenant.getDateOfBirth(),
                tenant.getAddress(),
                tenant.getCccd(),
                tenant.getPhone(),
                request.getPhysicalRoom() == null
                        ? room.getTitle()
                        : room.getTitle() + " - Phòng " + request.getPhysicalRoom().getRoomNumber(),
                room.getAddress(),
                room.getSize(),
                room.getCapacity(),
                room.getPrice(),
                request.getMoveInDate()
        );
    }

    @Transactional(readOnly = true)
    public RentalContract getDownloadableContract(User actor, Long requestId) {
        RentalContract contract = rentalContractRepository.findByRentalRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Hợp đồng chưa được tạo"));
        boolean participant = contract.getLandlord().getId().equals(actor.getId())
                || contract.getTenant().getId().equals(actor.getId());
        if (!participant && actor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Bạn không có quyền tải hợp đồng này");
        }
        return contract;
    }

    private void createContractIfNeeded(RentalRequest request, InteractionController.RentalStatusRequest payload) {
        if (rentalContractRepository.findByRentalRequestId(request.getId()).isPresent()) {
            return;
        }
        if (payload.startDate() == null || payload.endDate() == null) {
            throw new IllegalArgumentException("Vui lòng nhập ngày bắt đầu và ngày kết thúc hợp đồng");
        }
        if (!payload.endDate().isAfter(payload.startDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu hợp đồng");
        }
        if (payload.deposit() == null || payload.deposit() < 0) {
            throw new IllegalArgumentException("Tiền cọc không hợp lệ");
        }
        if (payload.rent() == null || payload.rent() <= 0) {
            throw new IllegalArgumentException("Tiền thuê không hợp lệ");
        }
        if (payload.paymentCycle() == null || payload.paymentCycle().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập kỳ thanh toán");
        }
        String tenantCccd = normalizeOptional(payload.tenantCccd());
        if (tenantCccd == null) {
            tenantCccd = normalizeOptional(request.getTenant().getCccd());
        }
        if (tenantCccd == null) {
            throw new IllegalArgumentException("Vui lòng nhập CCCD người thuê");
        }
        String tenantAddress = normalizeOptional(payload.tenantAddress());
        if (tenantAddress == null) {
            tenantAddress = normalizeOptional(request.getTenant().getAddress());
        }
        if (tenantAddress == null) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ thường trú");
        }

        RentalContract contract = new RentalContract();
        contract.setRentalRequest(request);
        contract.setRoom(request.getRoom());
        contract.setPhysicalRoom(request.getPhysicalRoom());
        contract.setTenant(request.getTenant());
        contract.setLandlord(request.getLandlord());
        contract.setStartDate(payload.startDate());
        contract.setEndDate(payload.endDate());
        contract.setDeposit(payload.deposit());
        contract.setRent(payload.rent());
        contract.setPaymentCycle(payload.paymentCycle().trim());
        contract.setTenantCccd(tenantCccd);
        contract.setTenantAddress(tenantAddress);
        contract.setStatus(ContractStatus.ACTIVE);
        rentalContractRepository.save(contract);

        TemporaryResidence residence = new TemporaryResidence();
        residence.setContract(contract);
        residence.setRoom(request.getRoom());
        residence.setPhysicalRoom(request.getPhysicalRoom());
        residence.setTenant(request.getTenant());
        residence.setLandlord(request.getLandlord());
        residence.setType(payload.residenceType() != null ? payload.residenceType() : ResidenceType.TEMPORARY_RESIDENCE);
        String residenceAddress = normalizeOptional(payload.residenceAddress());
        if (residenceAddress == null) {
            residenceAddress = tenantAddress;
        }
        residence.setAddress(residenceAddress);
        residence.setStartDate(payload.startDate());
        residence.setEndDate(payload.endDate());
        residence.setStatus(ResidenceStatus.ACTIVE);
        temporaryResidenceRepository.save(residence);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isBlank()) return null;
        return value.trim();
    }

    @Transactional
    public int expirePendingHolds() {
        List<RentalRequest> expired = rentalRequestRepository
                .findByStatusAndExpiresAtBefore(RentalStatus.PENDING, Instant.now());
        expired.forEach(this::expireRequest);
        return expired.size();
    }

    private void expireRequest(RentalRequest request) {
        if (request.getPhysicalRoom() != null) {
            releasePhysicalRoom(request);
        }
        request.setStatus(RentalStatus.EXPIRED);
        request.setUpdatedAt(Instant.now());
    }

    private void releasePhysicalRoom(RentalRequest request) {
        PhysicalRoom physicalRoom = physicalRoomRepository.findWithLockById(request.getPhysicalRoom().getId())
                .orElse(null);
        if (physicalRoom != null && physicalRoom.getStatus() == PhysicalRoomStatus.HELD) {
            physicalRoom.setStatus(PhysicalRoomStatus.AVAILABLE);
            clearHold(physicalRoom);
            roomLayoutService.syncRoomCounts(request.getRoom().getId());
        }
    }

    private void releaseExpiredHoldIfNeeded(PhysicalRoom physicalRoom) {
        if (physicalRoom.getStatus() == PhysicalRoomStatus.HELD
                && physicalRoom.getHoldExpiresAt() != null
                && !physicalRoom.getHoldExpiresAt().isAfter(Instant.now())) {
            physicalRoom.setStatus(PhysicalRoomStatus.AVAILABLE);
            clearHold(physicalRoom);
        }
    }

    private void clearHold(PhysicalRoom physicalRoom) {
        physicalRoom.setHeldBy(null);
        physicalRoom.setHoldExpiresAt(null);
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
