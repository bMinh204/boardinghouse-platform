package com.trototn.boardinghouse.interaction;

import com.trototn.boardinghouse.interaction.domain.RentalStatus;
import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import com.trototn.boardinghouse.interaction.InteractionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {
    private final InteractionService interactionService;
    private final UserRepository userRepository;

    public InteractionController(InteractionService interactionService, UserRepository userRepository) {
        this.interactionService = interactionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/conversations")
    public Map<String, Object> listConversations(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return Map.of("conversations", interactionService.listConversations(user));
    }

    @GetMapping("/conversations/{id}")
    public Map<String, Object> getConversation(Principal principal, @PathVariable Long id) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Responses.ConversationView view = interactionService.getConversation(user, id);
        return Map.of("conversation", view);
    }

    @PostMapping("/conversations")
    @PreAuthorize("hasRole('TENANT')")
    public Map<String, Object> createConversation(Principal principal, @RequestBody ConversationRequest request) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Responses.ConversationView view = interactionService.createConversation(user, request.roomId(), request.content());
        return Map.of("conversation", view);
    }

    @PostMapping("/conversations/{id}/messages")
    public Map<String, Object> sendMessage(Principal principal, @PathVariable Long id, @RequestBody MessageRequest request) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Responses.ConversationView view = interactionService.sendMessage(user, id, request.content());
        return Map.of("conversation", view);
    }

    @GetMapping("/rental-requests")
    public Map<String, Object> listRentals(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return Map.of("rentalRequests", interactionService.listRentalRequests(user));
    }

    @PostMapping("/rooms/{roomId}/rental-requests")
    @PreAuthorize("hasRole('TENANT')")
    public Map<String, Object> createRental(Principal principal, @PathVariable Long roomId, @RequestBody RentalRequest request) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Responses.RentalRequestView view = interactionService.createRentalRequest(user, roomId, request.moveInDate(), request.note());
        return Map.of("rentalRequest", view);
    }

    @PatchMapping("/rental-requests/{id}")
    public ResponseEntity<?> updateRental(Principal principal, @PathVariable Long id, @RequestBody RentalStatusRequest request) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        interactionService.updateRentalStatus(user, id, request.status());
        return ResponseEntity.ok(Map.of("message", "updated"));
    }

    @PostMapping("/rooms/{roomId}/survey")
    public Map<String, Object> addSurvey(Principal principal, @PathVariable Long roomId, @RequestBody SurveyRequest request) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Responses.RoomView view = interactionService.addSurvey(user, roomId,
                request.cleanlinessRating(), request.securityRating(), request.convenienceRating(), request.comment());
        return Map.of("room", view);
    }

    public record ConversationRequest(@NotNull Long roomId, String content) {}

    public record MessageRequest(@NotBlank String content) {}

    public record RentalRequest(@NotNull LocalDate moveInDate, @NotBlank String note) {}

    public record RentalStatusRequest(@NotNull RentalStatus status) {}

    public record SurveyRequest(int cleanlinessRating, int securityRating, int convenienceRating, String comment) {}
}
