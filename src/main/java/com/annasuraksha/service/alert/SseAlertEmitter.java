package com.annasuraksha.service.alert;

import com.annasuraksha.model.alert.AlertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseAlertEmitter {

    private record EmitterEntry(SseEmitter emitter, String stateCode, String userId) {}

    private final List<EmitterEntry> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public SseEmitter subscribe(String stateCode, String userId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        EmitterEntry entry = new EmitterEntry(emitter, stateCode, userId);
        emitters.add(entry);

        emitter.onCompletion(() -> emitters.remove(entry));
        emitter.onTimeout(()    -> emitters.remove(entry));
        emitter.onError(e ->    { emitters.remove(entry); });

        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of("status", "connected", "userId", userId != null ? userId : "anonymous")));
        } catch (IOException e) {
            emitters.remove(entry);
        }
        log.info("SSE connected: {} | state={}", userId, stateCode);
        return emitter;
    }

    public void broadcast(AlertEvent alert) {
        List<EmitterEntry> dead = new ArrayList<>();
        for (EmitterEntry entry : emitters) {
            if (entry.stateCode() != null && alert.getStateCode() != null
                    && !entry.stateCode().equals(alert.getStateCode())) continue;
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("alertId",    alert.getAlertId());
                payload.put("alertType",  alert.getAlertType());
                payload.put("severity",   alert.getSeverity());
                payload.put("title",      alert.getTitle());
                payload.put("description",alert.getDescription());
                payload.put("entityId",   alert.getEntityId() != null ? alert.getEntityId() : "");
                payload.put("actionUrl",  alert.getActionUrl() != null ? alert.getActionUrl() : "");
                payload.put("createdAt",  alert.getCreatedAt().toString());
                entry.emitter().send(SseEmitter.event()
                    .id(alert.getAlertId())
                    .name(alert.getAlertType().toLowerCase())
                    .data(mapper.writeValueAsString(payload)));
            } catch (IOException e) {
                dead.add(entry);
            }
        }
        emitters.removeAll(dead);
    }

    public int activeConnections() { return emitters.size(); }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SseAlertEmitter.class);
}
