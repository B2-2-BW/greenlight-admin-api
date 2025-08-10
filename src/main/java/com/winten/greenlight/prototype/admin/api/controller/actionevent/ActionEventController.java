package com.winten.greenlight.prototype.admin.api.controller.actionevent;

import com.winten.greenlight.prototype.admin.domain.actionevent.ActionEventService;
import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("action-events")
@RequiredArgsConstructor
public class ActionEventController {
    private final ActionEventService actionEventService;

    @GetMapping(value= "/traffic/summary")
    public Object getCurrentTrafficSummary(@AuthenticationPrincipal CurrentUser currentUser) {
        return actionEventService.getCurrentTrafficSummary();
    }

    @GetMapping(value = "/traffic/sse/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletResponse response) {
        // 타임아웃 30초 기본, 필요시 새 값 지정 (예: 60초)
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간
        // 캐시 금지 및 keep-alive
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Connection", "keep-alive");

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        // 0초 후 시작, 2초마다 전송
        exec.scheduleAtFixedRate(() -> {
            try {
                Object result = actionEventService.getCurrentTraffic();
                emitter.send(SseEmitter.event()
                        .name("tick")
                        .data(result)); // data: ... 형태로 전송됨
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, 2, TimeUnit.SECONDS);

        // 스트림 종료/오류/타임아웃 시 스케줄러 정리
        emitter.onCompletion(exec::shutdown);
        emitter.onTimeout(() -> {
            exec.shutdown();
            emitter.complete();
        });
        emitter.onError((ex) -> exec.shutdown());

        return emitter;
    }
}