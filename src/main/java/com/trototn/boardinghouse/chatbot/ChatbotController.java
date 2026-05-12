package com.trototn.boardinghouse.chatbot;

import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.chatbot.ChatbotService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public Map<String, Object> ask(@RequestBody ChatbotRequest request) {
        String prompt = request.prompt() == null ? "" : request.prompt();
        Responses.ChatbotReply reply = chatbotService.reply(prompt);
        Map<String, Object> body = new HashMap<>();
        body.put("reply", reply.reply());
        body.put("budget", reply.budget());
        body.put("area", reply.area());
        body.put("amenity", reply.amenity());
        body.put("suggestions", reply.suggestions());
        return body;
    }

    public record ChatbotRequest(@NotBlank String prompt) {}
}
