package com.trototn.boardinghouse.room;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.common.MapperUtil;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.interaction.domain.Survey;
import com.trototn.boardinghouse.interaction.repository.FavoriteRepository;
import com.trototn.boardinghouse.interaction.repository.SurveyRepository;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.RoomStatus;
import com.trototn.boardinghouse.room.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final FavoriteRepository favoriteRepository;
    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public RoomService(RoomRepository roomRepository, FavoriteRepository favoriteRepository,
                       SurveyRepository surveyRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.favoriteRepository = favoriteRepository;
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Responses.RoomView> listRooms(User currentUser, Filters filters) {
        List<Room> rooms = roomRepository.findAll();
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        rooms = rooms.stream()
                .filter(r -> isAdmin || r.getModerationStatus() == ModerationStatus.APPROVED ||
                        (currentUser != null && r.getOwner().getId().equals(currentUser.getId())))
                .filter(r -> filters.keyword == null || containsIgnoreCase(r.getTitle(), filters.keyword) || containsIgnoreCase(r.getAddress(), filters.keyword))
                .filter(r -> filters.areaName == null || containsIgnoreCase(r.getAreaName(), filters.areaName))
                .filter(r -> filters.minPrice == null || (r.getPrice() != null && r.getPrice() >= filters.minPrice))
                .filter(r -> filters.maxPrice == null || (r.getPrice() != null && r.getPrice() <= filters.maxPrice))
                .filter(r -> filters.minSize == null || (r.getSize() != null && r.getSize() >= filters.minSize))
                .filter(r -> filters.maxSize == null || (r.getSize() != null && r.getSize() <= filters.maxSize))
                .filter(r -> filters.amenity == null || (r.getAmenities() != null && r.getAmenities().stream().anyMatch(a -> containsIgnoreCase(a, filters.amenity))))
                .collect(Collectors.toList());

        final List<Long> favIdsFinal = (currentUser != null && currentUser.getRole() == Role.TENANT)
                ? favoriteRepository.findByTenantId(currentUser.getId()).stream().map(f -> f.getRoom().getId()).toList()
                : Collections.emptyList();
        return rooms.stream()
                .map(r -> MapperUtil.toRoomView(r, favIdsFinal.contains(r.getId()), Collections.emptyList()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Responses.RoomView> listOwnerRooms(User owner) {
        return roomRepository.findByOwner(owner).stream()
                .map(room -> MapperUtil.toRoomView(room, false, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Responses.RoomView getRoomDetail(User currentUser, Long id) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        boolean isOwner = currentUser != null && room.getOwner().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner && room.getModerationStatus() != ModerationStatus.APPROVED) {
            throw new IllegalArgumentException("Room not available");
        }
        room.setViewCount(Optional.ofNullable(room.getViewCount()).orElse(0L) + 1);
        boolean favorite = false;
        if (currentUser != null && currentUser.getRole() == Role.TENANT) {
            favorite = favoriteRepository.findByTenantAndRoom(currentUser, room).isPresent();
        }
        List<Survey> surveys = surveyRepository.findByRoomId(room.getId());
        Responses.RoomView view = MapperUtil.toRoomView(room, favorite, surveys);
        return view;
    }

    @Transactional
    public Responses.RoomView createRoom(User owner, Room payload) {
        sanitizeRoom(payload);
        validateRoom(payload);
        payload.setOwner(owner);
        payload.setModerationStatus(owner.getRole() == Role.ADMIN ? ModerationStatus.APPROVED : ModerationStatus.PENDING);
        if (payload.getAvailableFrom() == null) {
            payload.setAvailableFrom(LocalDate.now());
        }
        Room saved = roomRepository.save(payload);
        return MapperUtil.toRoomView(saved, false, Collections.emptyList());
    }

    @Transactional
    public Responses.RoomView updateRoom(User owner, Long id, Room payload) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        assertCanManageRoom(owner, room);
        sanitizeRoom(payload);
        validateRoom(payload);
        room.setPropertyName(payload.getPropertyName());
        room.setTitle(payload.getTitle());
        room.setAddress(payload.getAddress());
        room.setAreaName(payload.getAreaName());
        room.setPrice(payload.getPrice());
        room.setSize(payload.getSize());
        room.setCapacity(payload.getCapacity());
        room.setAmenities(payload.getAmenities());
        room.setFeaturedImage(payload.getFeaturedImage());
        room.setImageUrls(payload.getImageUrls());
        room.setDescription(payload.getDescription());
        room.setContractNote(payload.getContractNote());
        room.setMapQuery(payload.getMapQuery());
        room.setContactPhone(payload.getContactPhone());
        room.setAvailableFrom(payload.getAvailableFrom());
        room.setStatus(payload.getStatus());
        if (owner.getRole() != Role.ADMIN) {
            room.setModerationStatus(ModerationStatus.PENDING);
        } else {
            room.setModerationStatus(payload.getModerationStatus());
        }
        Room saved = roomRepository.save(room);
        return MapperUtil.toRoomView(saved, false, Collections.emptyList());
    }

    @Transactional
    public void deleteRoom(User currentUser, Long id) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        assertCanManageRoom(currentUser, room);
        deleteRoomDependencies(id);
        roomRepository.delete(room);
    }

    private void deleteRoomDependencies(Long roomId) {
        executeDelete("delete m from messages m join conversations c on m.conversation_id = c.id where c.room_id = :roomId", roomId);
        executeDelete("delete cm from chat_messages cm join conversations c on cm.conversation_id = c.id where c.room_id = :roomId", roomId);
        executeDelete("delete from conversations where room_id = :roomId", roomId);
        executeDelete("delete from favorites where room_id = :roomId", roomId);
        executeDelete("delete from surveys where room_id = :roomId", roomId);
        executeDelete("delete from rental_requests where room_id = :roomId", roomId);
        executeDelete("delete from room_views where room_id = :roomId", roomId);
        executeDelete("delete from room_amenities where room_id = :roomId", roomId);
        executeDelete("delete from room_images where room_id = :roomId", roomId);
    }

    private void executeDelete(String sql, Long roomId) {
        entityManager.createNativeQuery(sql)
                .setParameter("roomId", roomId)
                .executeUpdate();
    }

    @Transactional
    public void updateStatus(User currentUser, Long id, RoomStatus status) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        assertCanManageRoom(currentUser, room);
        room.setStatus(status);
    }

    private void assertCanManageRoom(User actor, Room room) {
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        boolean isOwner = room.getOwner() != null && room.getOwner().getId().equals(actor.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Not allowed");
        }
    }

    @Transactional
    public void moderate(User admin, Long id, ModerationStatus moderationStatus, String note) {
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Not allowed");
        }
        Room room = roomRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        room.setModerationStatus(moderationStatus);
    }

    private boolean containsIgnoreCase(String source, String term) {
        if (source == null || term == null) return false;
        return source.toLowerCase().contains(term.toLowerCase());
    }

    private void validateRoom(Room room) {
        if (room.getPrice() == null || room.getPrice() <= 0) {
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0");
        }
        if (room.getSize() == null || room.getSize() <= 0) {
            throw new IllegalArgumentException("Diện tích phải lớn hơn 0");
        }
        if (room.getCapacity() == null || room.getCapacity() <= 0) {
            throw new IllegalArgumentException("Số người phải lớn hơn 0");
        }
        boolean hasAnyImage = isValidImageUrl(room.getFeaturedImage()) ||
                (room.getImageUrls() != null && room.getImageUrls().stream().anyMatch(this::isValidImageUrl));
        if (!hasAnyImage) {
            throw new IllegalArgumentException("Cần ít nhất 1 ảnh phòng");
        }
        if (room.getContactPhone() == null || room.getContactPhone().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số liên hệ");
        }
    }

    private void sanitizeRoom(Room room) {
        if (room.getFeaturedImage() != null) {
            String featuredImage = room.getFeaturedImage().trim();
            room.setFeaturedImage(isValidImageUrl(featuredImage) ? featuredImage : null);
        }
        if (room.getAmenities() != null) {
            room.setAmenities(room.getAmenities().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .toList());
        }
        if (room.getImageUrls() != null) {
            room.setImageUrls(room.getImageUrls().stream()
                    .filter(this::isValidImageUrl)
                    .map(String::trim)
                    .toList());
        }
        if ((room.getFeaturedImage() == null || room.getFeaturedImage().isBlank())
                && room.getImageUrls() != null && !room.getImageUrls().isEmpty()) {
            room.setFeaturedImage(room.getImageUrls().get(0));
        }
    }

    private boolean isValidImageUrl(String value) {
        if (value == null || value.isBlank()) return false;
        String trimmed = value.trim().toLowerCase();
        return trimmed.startsWith("http://")
                || trimmed.startsWith("https://")
                || trimmed.startsWith("/uploads/")
                || trimmed.startsWith("data:image/");
    }

    public record Filters(String keyword, String areaName, Long minPrice, Long maxPrice,
                          Double minSize, Double maxSize, String amenity) {}
}
