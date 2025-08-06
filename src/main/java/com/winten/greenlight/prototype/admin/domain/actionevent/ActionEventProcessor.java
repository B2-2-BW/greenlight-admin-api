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
    private final WriteApi writeApi;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final ObjectMapper objectMapper;

    @Value("${redis.key-prefix}")
    private String prefix;
    private String actionEventMeasurementKey;

    @PostConstruct
    public void init() {
        // influxDB는 [A-z,_]를 권장
        // https://docs.influxdata.com/influxdb/v1/concepts/schema_and_data_layout/
        actionEventMeasurementKey = prefix + "_action_event";
    }

    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void process(Map<String, String> content) {
        ActionEvent actionEvent = objectMapper.convertValue(content, ActionEvent.class);
        Point point = this.createInfluxPoint(actionEvent);
        writeApi.writePoint(point);
    }

    /**
     * 수집된 데이터를 기반으로 InfluxDB에 저장할 Point 객체를 생성
     */
    // TODO ObjectMapper로 ActionEvent에 매핑
    private Point createInfluxPoint(ActionEvent actionEvent) {
        Point point = Point.measurement(actionEventMeasurementKey)
                .addTag("eventType", actionEvent.getEventType().name())
                .addTag("actionGroupId", actionEvent.getActionGroupId().toString())
                .addTag("actionId", actionEvent.getActionId().toString())
                .addField("customerId", actionEvent.getCustomerId())
                .addField("eventTimestamp", actionEvent.getEventTimestamp())
                .addField("count", 1)
                .time(Instant.now(), WritePrecision.MS);

        if (actionEvent.getWaitTimeMs() != null) {
            point.addField("waitTimeMs", actionEvent.getWaitTimeMs());
        }

        return point;
    }

    @Recover
    public void recover(Exception e, Map<String, String> content) {
        log.error("All retries failed for message content. Moving to DLQ.", e);
        // Dead Letter Queue 로직: 실패한 메시지를 다른 Redis Stream이나 List에 저장
        redisTemplate.opsForStream().add(keyBuilder.actionEventDlqStream(), content);
    }
}