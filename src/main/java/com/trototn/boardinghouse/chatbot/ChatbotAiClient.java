package com.trototn.boardinghouse.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trototn.boardinghouse.common.dto.Responses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ChatbotAiClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String provider;
    private final String openAiApiKey;
    private final String openAiModel;
    private final String geminiApiKey;
    private final String geminiModel;

    public ChatbotAiClient(ObjectMapper objectMapper,
                           @Value("${app.chatbot.provider:local}") String provider,
                           @Value("${app.chatbot.openai.api-key:}") String openAiApiKey,
                           @Value("${app.chatbot.openai.model:gpt-4o-mini}") String openAiModel,
                           @Value("${app.chatbot.gemini.api-key:}") String geminiApiKey,
                           @Value("${app.chatbot.gemini.model:gemini-2.5-flash}") String geminiModel) {
        this.objectMapper = objectMapper;
        this.provider = provider == null ? "local" : provider.trim().toLowerCase();
        this.openAiApiKey = openAiApiKey == null ? "" : openAiApiKey.trim();
        this.openAiModel = openAiModel;
        this.geminiApiKey = geminiApiKey == null ? "" : geminiApiKey.trim();
        this.geminiModel = geminiModel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public Optional<String> generateReply(String userPrompt, String localReply, List<Responses.RoomView> suggestions) {
        try {
            if ("openai".equals(provider) && !openAiApiKey.isBlank()) {
                return callOpenAi(userPrompt, localReply, suggestions);
            }
            if ("gemini".equals(provider) && !geminiApiKey.isBlank()) {
                return callGemini(userPrompt, localReply, suggestions);
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<String> callOpenAi(String userPrompt, String localReply, List<Responses.RoomView> suggestions) throws Exception {
        Map<String, Object> body = Map.of(
                "model", openAiModel,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", buildPrompt(userPrompt, localReply, suggestions))
                )
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("choices").path(0).path("message").path("content").asText("");
        return clean(text);
    }

    private Optional<String> callGemini(String userPrompt, String localReply, List<Responses.RoomView> suggestions) throws Exception {
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt()))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", buildPrompt(userPrompt, localReply, suggestions))))),
                "generationConfig", Map.of("temperature", 0.2)
        );
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("x-goog-api-key", geminiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        return clean(text);
    }

    private String systemPrompt() {
        return """
                Bạn là trợ lý tìm phòng trọ cho nền tảng Trọ Tốt ICTU.
                Chỉ tư vấn dựa trên danh sách phòng được cung cấp.
                Không bịa phòng, giá, địa chỉ, tiện nghi hoặc trạng thái.
                Trả lời tiếng Việt có dấu, ngắn gọn, thực dụng.
                Nếu không có phòng khớp tuyệt đối, nói rõ đang gợi ý gần đúng.
                """;
    }

    private String buildPrompt(String userPrompt, String localReply, List<Responses.RoomView> suggestions) {
        StringBuilder builder = new StringBuilder();
        builder.append("Người dùng hỏi: ").append(userPrompt == null ? "" : userPrompt).append("\n");
        builder.append("Kết quả lọc nội bộ: ").append(localReply).append("\n");
        builder.append("Danh sách phòng ứng viên:\n");
        if (suggestions == null || suggestions.isEmpty()) {
            builder.append("- Không có phòng ứng viên.\n");
        } else {
            for (Responses.RoomView room : suggestions) {
                builder.append("- ID ").append(room.id())
                        .append(": ").append(room.title())
                        .append(" | Giá: ").append(room.price())
                        .append(" | Khu vực: ").append(room.areaName())
                        .append(" | Địa chỉ: ").append(room.address())
                        .append(" | Diện tích: ").append(room.size())
                        .append(" | Sức chứa: ").append(room.capacity())
                        .append(" | Tiện nghi: ").append(room.amenities())
                        .append(" | Trạng thái: ").append(room.status())
                        .append("\n");
            }
        }
        builder.append("Hãy trả lời như một trợ lý tư vấn phòng trọ, nêu 1-3 lựa chọn tốt nhất nếu có.");
        return builder.toString();
    }

    private Optional<String> clean(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(text.trim());
    }
}
