package com.example.backend.admin.service;

import com.example.backend.admin.event.UserLoginEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminEventBus {
    private final Map<String, SseEmitter> clients = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String id) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());
        clients.put(id, emitter);
        emitter.onCompletion(() -> clients.remove(id));
        emitter.onTimeout(() -> clients.remove(id));
        try { emitter.send(SseEmitter.event().name("ready").data("ok")); } catch (IOException ignored) {}
        return emitter;
    }

    public void publish(UserLoginEvent event) {
        clients.forEach((k, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("user-login").data(event));
            } catch (IOException e) {
                emitter.complete();
                clients.remove(k);
            }
        });
    }
}
