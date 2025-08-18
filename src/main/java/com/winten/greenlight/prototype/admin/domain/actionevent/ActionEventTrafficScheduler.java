package com.winten.greenlight.prototype.admin.domain.actionevent;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class ActionEventTrafficScheduler {
    private final ActionEventService actionEventService;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final long TIMEOUT_MS = 60 * 30 * 1000L;

    private String newId() {
        return UUID.randomUUID().toString();
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        String id = newId();

        emitters.put(id, emitter);

        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        emitter.onError(ex -> emitters.remove(id));

        return emitter;
    }

    @PostConstruct
    public void init() {
        executor.scheduleAtFixedRate(() -> {
            Object result = actionEventService.getCurrentTraffic();
            for (Map.Entry<String, SseEmitter> emitterEntry : emitters.entrySet()) {
                SseEmitter emitter = emitterEntry.getValue();
                try {
                    emitter.send(SseEmitter.event().name("tick").data(result));
                } catch (IOException ex) {
                    emitter.complete();
                    emitters.remove(emitterEntry.getKey());
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
}