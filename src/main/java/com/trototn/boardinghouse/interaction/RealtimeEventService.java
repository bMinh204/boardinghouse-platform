package com.trototn.boardinghouse.interaction;

import com.trototn.boardinghouse.auth.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeEventService {
    private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(Long userId, Role role) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        Subscriber subscriber = new Subscriber(userId, role, emitter);
        subscribers.add(subscriber);
        emitter.onCompletion(() -> subscribers.remove(subscriber));
        emitter.onTimeout(() -> subscribers.remove(subscriber));
        emitter.onError(error -> subscribers.remove(subscriber));
        try {
            emitter.send(SseEmitter.event().name("CONNECTED").data(Map.of("status", "ok")));
        } catch (IOException | IllegalStateException ex) {
            subscribers.remove(subscriber);
        }
        return emitter;
    }

    public void publishNewMessage(Long conversationId, String senderName, Long tenantId, Long landlordId) {
        Map<String, Object> payload = Map.of(
                "conversationId", conversationId,
                "senderName", senderName == null ? "Nguoi dung" : senderName
        );
        for (Subscriber subscriber : subscribers) {
            if (!subscriber.canReceive(tenantId, landlordId)) {
                continue;
            }
            try {
                subscriber.emitter().send(SseEmitter.event().name("NEW_MESSAGE").data(payload));
            } catch (IOException | IllegalStateException ex) {
                subscribers.remove(subscriber);
            }
        }
    }

    private record Subscriber(Long userId, Role role, SseEmitter emitter) {
        boolean canReceive(Long tenantId, Long landlordId) {
            if (role == Role.ADMIN) return true;
            return userId != null && (userId.equals(tenantId) || userId.equals(landlordId));
        }
    }
}
