package com.winten.greenlight.admin.domain.actionevent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.admin.domain.actionevent.dto.ActionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionEventProcessor {
    private final ObjectMapper objectMapper;
    private final ActionEventQueue actionEventQueue;

    // TODO queue에서 배치로 처리되기 때문에 재시도 전략이 없음. 구조 개선 필요
    public void process(Map<String, String> content) {
        ActionEvent actionEvent = objectMapper.convertValue(content, ActionEvent.class);
        actionEventQueue.offer(actionEvent);
    }
}