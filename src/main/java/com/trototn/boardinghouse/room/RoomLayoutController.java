package com.trototn.boardinghouse.room;

import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.room.domain.PhysicalRoomStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms/{roomId}/layout")
public class RoomLayoutController {
    private final RoomLayoutService layoutService;
    private final UserRepository userRepository;

    public RoomLayoutController(RoomLayoutService layoutService, UserRepository userRepository) {
        this.layoutService = layoutService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Map<String, Object> getLayout(Principal principal, @PathVariable Long roomId) {
        User actor = principal == null ? null : userRepository.findByEmail(principal.getName()).orElse(null);
        return Map.of(
                "layout", layoutService.getLayout(actor, roomId),
                "canManage", layoutService.canManage(actor, roomId));
    }

    @PostMapping("/sections")
    public Map<String, Object> addSection(Principal principal, @PathVariable Long roomId,
            @RequestBody SectionRequest request) {
        return Map.of("layout", layoutService.addSection(
                currentUser(principal), roomId, request.name(), request.displayOrder()));
    }

    @PatchMapping("/sections/{sectionId}")
    public Map<String, Object> updateSection(Principal principal, @PathVariable Long roomId,
            @PathVariable Long sectionId, @RequestBody SectionRequest request) {
        return Map.of("layout", layoutService.updateSection(
                currentUser(principal), roomId, sectionId, request.name(), request.displayOrder()));
    }

    @DeleteMapping("/sections/{sectionId}")
    public Map<String, Object> deleteSection(Principal principal, @PathVariable Long roomId,
            @PathVariable Long sectionId) {
        return Map.of("layout", layoutService.deleteSection(currentUser(principal), roomId, sectionId));
    }

    @PostMapping("/physical-rooms")
    public Map<String, Object> addPhysicalRoom(Principal principal, @PathVariable Long roomId,
            @RequestBody PhysicalRoomRequest request) {
        return Map.of("layout", layoutService.addPhysicalRoom(
                currentUser(principal), roomId, request.sectionId(), request.roomNumber(),
                request.displayOrder(), request.status()));
    }

    @PatchMapping("/physical-rooms/{physicalRoomId}")
    public Map<String, Object> updatePhysicalRoom(Principal principal, @PathVariable Long roomId,
            @PathVariable Long physicalRoomId, @RequestBody PhysicalRoomRequest request) {
        return Map.of("layout", layoutService.updatePhysicalRoom(
                currentUser(principal), roomId, physicalRoomId, request.sectionId(),
                request.roomNumber(), request.displayOrder(), request.status()));
    }

    @DeleteMapping("/physical-rooms/{physicalRoomId}")
    public Map<String, Object> deletePhysicalRoom(Principal principal, @PathVariable Long roomId,
            @PathVariable Long physicalRoomId) {
        return Map.of("layout", layoutService.deletePhysicalRoom(
                currentUser(principal), roomId, physicalRoomId));
    }

    private User currentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName()).orElseThrow();
    }

    public record SectionRequest(String name, Integer displayOrder) {
    }

    public record PhysicalRoomRequest(Long sectionId, String roomNumber,
                                      Integer displayOrder, PhysicalRoomStatus status) {
    }
}
