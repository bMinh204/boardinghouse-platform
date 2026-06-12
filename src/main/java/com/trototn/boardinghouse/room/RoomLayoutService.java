package com.trototn.boardinghouse.room;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.room.domain.PhysicalRoom;
import com.trototn.boardinghouse.room.domain.PhysicalRoomStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.RoomSection;
import com.trototn.boardinghouse.room.domain.RoomStatus;
import com.trototn.boardinghouse.room.repository.PhysicalRoomRepository;
import com.trototn.boardinghouse.room.repository.RoomRepository;
import com.trototn.boardinghouse.room.repository.RoomSectionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomLayoutService {
    private final RoomRepository roomRepository;
    private final RoomSectionRepository sectionRepository;
    private final PhysicalRoomRepository physicalRoomRepository;

    public RoomLayoutService(RoomRepository roomRepository, RoomSectionRepository sectionRepository,
            PhysicalRoomRepository physicalRoomRepository) {
        this.roomRepository = roomRepository;
        this.sectionRepository = sectionRepository;
        this.physicalRoomRepository = physicalRoomRepository;
    }

    @Transactional(readOnly = true)
    public Responses.RoomLayoutView getLayout(Long roomId) {
        return getLayout(null, roomId);
    }

    @Transactional(readOnly = true)
    public Responses.RoomLayoutView getLayout(User actor, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà trọ"));
        List<Responses.RoomSectionView> sections = sectionRepository
                .findByRoomIdOrderByDisplayOrderAscIdAsc(roomId).stream()
                .map(section -> new Responses.RoomSectionView(
                        section.getId(),
                        section.getName(),
                        section.getDisplayOrder(),
                        physicalRoomRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(section.getId()).stream()
                                .map(physicalRoom -> toView(physicalRoom, actor))
                                .toList()))
                .toList();
        int total = Math.toIntExact(physicalRoomRepository.countByRoomId(roomId));
        int available = Math.toIntExact(
                physicalRoomRepository.countByRoomIdAndStatus(roomId, PhysicalRoomStatus.AVAILABLE));
        int held = countByStatus(roomId, PhysicalRoomStatus.HELD);
        int occupied = countByStatus(roomId, PhysicalRoomStatus.OCCUPIED);
        int expiringSoon = countByStatus(roomId, PhysicalRoomStatus.EXPIRING_SOON);
        int maintenance = countByStatus(roomId, PhysicalRoomStatus.MAINTENANCE);
        return new Responses.RoomLayoutView(
                roomId, room.getPropertyName(), total, available, held, occupied,
                expiringSoon, maintenance, sections);
    }

    @Transactional(readOnly = true)
    public boolean canManage(User actor, Long roomId) {
        if (actor == null) {
            return false;
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà trọ"));
        return actor.getRole() == Role.ADMIN
                || room.getOwner() != null && room.getOwner().getId().equals(actor.getId());
    }

    @Transactional
    public Responses.RoomLayoutView addSection(User actor, Long roomId, String name, Integer displayOrder) {
        Room room = managedRoom(actor, roomId);
        String normalizedName = required(name, "Vui lòng nhập tên khu hoặc tầng");
        RoomSection section = new RoomSection();
        section.setRoom(room);
        section.setName(normalizedName);
        section.setDisplayOrder(displayOrder != null ? displayOrder
                : Math.toIntExact(sectionRepository.countByRoomId(roomId)));
        sectionRepository.save(section);
        return getLayout(roomId);
    }

    @Transactional
    public Responses.RoomLayoutView updateSection(User actor, Long roomId, Long sectionId,
            String name, Integer displayOrder) {
        managedRoom(actor, roomId);
        RoomSection section = sectionForRoom(sectionId, roomId);
        section.setName(required(name, "Vui lòng nhập tên khu hoặc tầng"));
        if (displayOrder != null) {
            section.setDisplayOrder(displayOrder);
        }
        return getLayout(roomId);
    }

    @Transactional
    public Responses.RoomLayoutView deleteSection(User actor, Long roomId, Long sectionId) {
        managedRoom(actor, roomId);
        RoomSection section = sectionForRoom(sectionId, roomId);
        physicalRoomRepository.deleteBySectionId(sectionId);
        sectionRepository.delete(section);
        syncRoomCounts(roomId);
        return getLayout(roomId);
    }

    @Transactional
    public Responses.RoomLayoutView addPhysicalRoom(User actor, Long roomId, Long sectionId,
            String roomNumber, Integer displayOrder, PhysicalRoomStatus status) {
        Room room = managedRoom(actor, roomId);
        RoomSection section = sectionForRoom(sectionId, roomId);
        String normalizedNumber = required(roomNumber, "Vui lòng nhập số phòng");
        if (physicalRoomRepository.existsByRoomIdAndRoomNumberIgnoreCase(roomId, normalizedNumber)) {
            throw new IllegalArgumentException("Số phòng đã tồn tại trong nhà trọ");
        }
        PhysicalRoom physicalRoom = new PhysicalRoom();
        physicalRoom.setRoom(room);
        physicalRoom.setSection(section);
        physicalRoom.setRoomNumber(normalizedNumber);
        physicalRoom.setDisplayOrder(displayOrder != null ? displayOrder
                : physicalRoomRepository.findBySectionIdOrderByDisplayOrderAscIdAsc(sectionId).size());
        if (status == PhysicalRoomStatus.HELD) {
            throw new IllegalArgumentException("Trạng thái đang giữ chỉ được tạo bởi yêu cầu của người thuê");
        }
        physicalRoom.setStatus(status != null ? status : PhysicalRoomStatus.AVAILABLE);
        physicalRoomRepository.save(physicalRoom);
        syncRoomCounts(roomId);
        return getLayout(roomId);
    }

    @Transactional
    public Responses.RoomLayoutView updatePhysicalRoom(User actor, Long roomId, Long physicalRoomId,
            Long sectionId, String roomNumber, Integer displayOrder, PhysicalRoomStatus status) {
        managedRoom(actor, roomId);
        PhysicalRoom physicalRoom = physicalRoomForRoom(physicalRoomId, roomId);
        if (sectionId != null) {
            physicalRoom.setSection(sectionForRoom(sectionId, roomId));
        }
        if (roomNumber != null) {
            String normalizedNumber = required(roomNumber, "Vui lòng nhập số phòng");
            if (!physicalRoom.getRoomNumber().equalsIgnoreCase(normalizedNumber)
                    && physicalRoomRepository.existsByRoomIdAndRoomNumberIgnoreCase(roomId, normalizedNumber)) {
                throw new IllegalArgumentException("Số phòng đã tồn tại trong nhà trọ");
            }
            physicalRoom.setRoomNumber(normalizedNumber);
        }
        if (displayOrder != null) {
            physicalRoom.setDisplayOrder(displayOrder);
        }
        if (status != null) {
            if (status == PhysicalRoomStatus.HELD) {
                throw new IllegalArgumentException("Trạng thái đang giữ chỉ được tạo bởi yêu cầu của người thuê");
            }
            physicalRoom.setStatus(status);
            physicalRoom.setHeldBy(null);
            physicalRoom.setHoldExpiresAt(null);
        }
        syncRoomCounts(roomId);
        return getLayout(roomId);
    }

    @Transactional
    public Responses.RoomLayoutView deletePhysicalRoom(User actor, Long roomId, Long physicalRoomId) {
        managedRoom(actor, roomId);
        PhysicalRoom physicalRoom = physicalRoomForRoom(physicalRoomId, roomId);
        physicalRoomRepository.delete(physicalRoom);
        syncRoomCounts(roomId);
        return getLayout(roomId);
    }

    private Responses.PhysicalRoomView toView(PhysicalRoom room, User actor) {
        return new Responses.PhysicalRoomView(
                room.getId(), room.getRoomNumber(), room.getDisplayOrder(), room.getStatus(),
                room.getHoldExpiresAt(),
                actor != null && room.getHeldBy() != null
                        && room.getHeldBy().getId().equals(actor.getId()));
    }

    private Room managedRoom(User actor, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà trọ"));
        boolean owner = room.getOwner() != null && room.getOwner().getId().equals(actor.getId());
        if (!owner && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền sửa sơ đồ nhà trọ này");
        }
        return room;
    }

    private RoomSection sectionForRoom(Long sectionId, Long roomId) {
        RoomSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khu hoặc tầng"));
        if (!section.getRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("Khu hoặc tầng không thuộc nhà trọ này");
        }
        return section;
    }

    private PhysicalRoom physicalRoomForRoom(Long physicalRoomId, Long roomId) {
        PhysicalRoom physicalRoom = physicalRoomRepository.findById(physicalRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));
        if (!physicalRoom.getRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("Phòng không thuộc nhà trọ này");
        }
        return physicalRoom;
    }

    public void syncRoomCounts(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        int total = Math.toIntExact(physicalRoomRepository.countByRoomId(roomId));
        int available = Math.toIntExact(
                physicalRoomRepository.countByRoomIdAndStatus(roomId, PhysicalRoomStatus.AVAILABLE));
        room.setTotalRooms(total);
        room.setAvailableRooms(available);
        if (total > 0 && (room.getStatus() == RoomStatus.AVAILABLE || room.getStatus() == RoomStatus.OCCUPIED)) {
            room.setStatus(available > 0 ? RoomStatus.AVAILABLE : RoomStatus.OCCUPIED);
        }
    }

    private int countByStatus(Long roomId, PhysicalRoomStatus status) {
        return Math.toIntExact(physicalRoomRepository.countByRoomIdAndStatus(roomId, status));
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
