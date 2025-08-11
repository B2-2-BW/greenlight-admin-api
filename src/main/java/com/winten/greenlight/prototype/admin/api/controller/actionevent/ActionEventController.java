package com.winten.greenlight.prototype.admin.api.controller.actionevent;

import com.winten.greenlight.prototype.admin.domain.actionevent.ActionEventService;
import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("action-events")
@RequiredArgsConstructor
public class ActionEventController {
    private final ActionEventService actionEventService;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final long TIMEOUT_MS = 60 * 60 * 1000L;


    @GetMapping(value= "/traffic/summary")
    public Object getCurrentTrafficSummary(@AuthenticationPrincipal CurrentUser currentUser) {
        return actionEventService.getCurrentTraffic();
    }

    @GetMapping(value = "/traffic/sse/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String clientId) {
        // 타임아웃 30초 기본, 필요시 새 값 지정 (예: 60초)
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS); // 1시간

        emitters.put(clientId, emitter);

        emitter.onCompletion(() -> cleanup(clientId));
        emitter.onTimeout(() -> {
            cleanup(clientId);
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        });
        emitter.onError(ex -> {
            cleanup(clientId);
        });

        executor.scheduleAtFixedRate(() -> {
            SseEmitter e = emitters.get(clientId);
            try {
                Object result = actionEventService.getCurrentTraffic();
                emitter.send(SseEmitter.event().name("tick").data(result));
            } catch (IOException ex) {
                cleanup(clientId);
            }
        }, 0, 2, TimeUnit.SECONDS);

        return emitter;
    }

    private void cleanup(String clientId) {
        var emitter = emitters.get(clientId);
        if (emitter != null) {
            emitter.complete();
        }
        emitters.remove(clientId);
    }
}