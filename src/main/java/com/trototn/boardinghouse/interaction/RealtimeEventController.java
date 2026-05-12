package com.trototn.boardinghouse.interaction;

import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

@RestController
@RequestMapping("/api/events")
public class RealtimeEventController {
    private final RealtimeEventService realtimeEventService;
    private final UserRepository userRepository;

    public RealtimeEventController(RealtimeEventService realtimeEventService, UserRepository userRepository) {
        this.realtimeEventService = realtimeEventService;
        this.userRepository = userRepository;
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return realtimeEventService.subscribe(user.getId(), user.getRole());
    }
}
