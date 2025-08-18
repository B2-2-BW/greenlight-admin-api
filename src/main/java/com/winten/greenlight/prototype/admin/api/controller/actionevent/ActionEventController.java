package com.winten.greenlight.prototype.admin.api.controller.actionevent;

import com.winten.greenlight.prototype.admin.domain.actionevent.ActionEventTrafficEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("action-events")
@RequiredArgsConstructor
public class ActionEventController {
    private final ActionEventTrafficEmitter trafficScheduler;

    @GetMapping(value = "/traffic/sse/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return trafficScheduler.createEmitter();
    }
}