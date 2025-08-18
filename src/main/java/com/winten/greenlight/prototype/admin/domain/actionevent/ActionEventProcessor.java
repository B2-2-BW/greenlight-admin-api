package com.winten.greenlight.prototype.admin.domain.actionevent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Instant;
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