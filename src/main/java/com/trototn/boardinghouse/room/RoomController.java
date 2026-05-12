package com.trototn.boardinghouse.room;

import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import com.trototn.boardinghouse.common.MapperUtil;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.interaction.InteractionService;
import com.trototn.boardinghouse.interaction.repository.FavoriteRepository;
import com.trototn.boardinghouse.room.domain.ModerationStatus;
import com.trototn.boardinghouse.room.domain.Room;
import com.trototn.boardinghouse.room.domain.RoomStatus;
import com.trototn.boardinghouse.room.repository.RoomRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService roomService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final FavoriteRepository favoriteRepository;
    private final InteractionService interactionService;

    public RoomController(RoomService roomService, UserRepository userRepository, RoomRepository roomRepository, FavoriteRepository favoriteRepository, InteractionService interactionService) {
        this.roomService = roomService;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.favoriteRepository = favoriteRepository;
        this.interactionService = interactionService;
    }

    @GetMapping
    public Map<String, Object> listRooms(Principal principal,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String areaName,
                                         @RequestParam(required = false) Long minPrice,
                                         @RequestParam(required = false) Long maxPrice,
                                         @RequestParam(required = false) Double minSize,
                                         @RequestParam(required = false) Double maxSize,
                                         @RequestParam(required = false) String amenity) {
        User user = principal == null ? null : userRepository.findByEmail(principal.getName()).orElse(null);
        List<Responses.RoomView> rooms = roomService.listRooms(user, new RoomService.Filters(keyword, areaName, minPrice, maxPrice, minSize, maxSize, amenity));
        return Map.of("rooms", rooms);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getRoom(Principal principal, @PathVariable Long id) {
        User user = principal == null ? null : userRepository.findByEmail(principal.getName()).orElse(null);
        Responses.RoomView room = roomService.getRoomDetail(user, id);
        return Map.of("room", room);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('LANDLORD')")
    public Map<String, Object> myRooms(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return Map.of("rooms", roomService.listOwnerRooms(user));
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasRole('TENANT')")
    public Map<String, Object> favoriteRooms(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return Map.of("rooms", roomService.listRooms(user, new RoomService.Filters(null, null, null, null, null, null, null))
                .stream().filter(r -> Boolean.TRUE.equals(r.favorite())).toList());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Map<String, Object> pendingRooms() {
        List<Room> rooms = roomRepository.findByModerationStatus(ModerationStatus.PENDING);
        List<Responses.RoomView> views = rooms.stream().map(r -> MapperUtil.toRoomView(r, false, List.of())).toList();
        return Map.of("rooms", views);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public Map<String, Object> createRoom(Principal principal, @RequestBody RoomRequest request) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Room room = request.toRoom();
        Responses.RoomView view = roomService.createRoom(user, room);
        return Map.of("room", view);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public Map<String, Object> updateRoom(Principal principal, @PathVariable Long id, @RequestBody RoomRequest request) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Responses.RoomView view = roomService.updateRoom(user, id, request.toRoom());
        return Map.of("room", view);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<?> deleteRoom(Principal principal, @PathVariable Long id) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        roomService.deleteRoom(user, id);
        return ResponseEntity.ok(Map.of("message", "deleted"));
    }

    @PostMapping("/{id}/favorite")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> toggleFavorite(Principal principal, @PathVariable Long id) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        interactionService.toggleFavorite(user, id);
        return ResponseEntity.ok(Map.of("message", "updated"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<?> updateStatus(Principal principal, @PathVariable Long id, @RequestBody StatusRequest request) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        roomService.updateStatus(user, id, request.status());
        return ResponseEntity.ok(Map.of("message", "updated"));
    }

    @PatchMapping("/{id}/moderation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> moderate(@PathVariable Long id, @RequestBody ModerationRequest request, Principal principal) {
        User admin = userRepository.findByEmail(principal.getName()).orElseThrow();
        roomService.moderate(admin, id, request.moderationStatus(), request.note());
        return ResponseEntity.ok(Map.of("message", "moderated"));
    }

    public record RoomRequest(
            @NotBlank String propertyName,
            @NotBlank String title,
            @NotBlank String address,
            @NotBlank String areaName,
            Long price,
            Double size,
            Integer capacity,
            List<String> amenities,
            String featuredImage,
            List<String> imageUrls,
            String description,
            String contractNote,
            String mapQuery,
            String contactPhone,
            RoomStatus status,
            ModerationStatus moderationStatus,
            LocalDate availableFrom
    ) {
        public Room toRoom() {
            Room r = new Room();
            r.setPropertyName(propertyName);
            r.setTitle(title);
            r.setAddress(address);
            r.setAreaName(areaName);
            r.setPrice(price);
            r.setSize(size);
            r.setCapacity(capacity);
            r.setAmenities(amenities);
            r.setFeaturedImage(featuredImage);
            r.setImageUrls(imageUrls);
            r.setDescription(description);
            r.setContractNote(contractNote);
            r.setMapQuery(mapQuery);
            r.setContactPhone(contactPhone);
            r.setStatus(status != null ? status : RoomStatus.AVAILABLE);
            r.setModerationStatus(moderationStatus != null ? moderationStatus : ModerationStatus.PENDING);
            r.setAvailableFrom(availableFrom);
            return r;
        }
    }

    public record StatusRequest(RoomStatus status) {}

    public record ModerationRequest(ModerationStatus moderationStatus, String note) {}
}
