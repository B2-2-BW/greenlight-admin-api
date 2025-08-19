package com.winten.greenlight.prototype.admin.domain.actionevent;

import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.winten.greenlight.prototype.admin.domain.actionevent.dto.ActionEvent;
import com.winten.greenlight.prototype.admin.domain.actionevent.dto.ActionEventTrafficResponse;
import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ActionEventScheduler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final WriteApi writeApi;
    private final ActionEventQueue actionEventQueue;
    private final ActionEventService actionEventService;
    private final ActionEventTrafficEmitter actionEventTrafficEmitter;

    private final int MAX_DRAIN = 10000;

    @Value("${redis.key-prefix}")
    private String prefix;
    private String actionEventMeasurementKey;

    @PostConstruct
    public void init() {
        // influxDB는 [A-z,_]를 권장
        // https://docs.influxdata.com/influxdb/v1/concepts/schema_and_data_layout/
        actionEventMeasurementKey = prefix + "_action_event";
    }

    @Scheduled(fixedRate = 60000)
    public void trimStream() {
        try {
            redisTemplate.opsForStream().trim(keyBuilder.actionEventStream(), 100_000);
        } catch (Exception e) {
            log.error("failed to trim stream", e);
        }
    }

    @Scheduled(fixedRate = 2000)
    public void writePointsAndEmit() {
        try {
            int size = actionEventQueue.size();

            List<ActionEvent> actionEvents = new ArrayList<>(Math.min(size, MAX_DRAIN));
            actionEventQueue.drainTo(actionEvents, MAX_DRAIN);

            try {
                ActionEventTrafficResponse response = actionEventService.makeTrafficResponse(actionEvents);
                actionEventTrafficEmitter.emit(response);
            } catch (Exception e) {
                log.error("failed to emit action events " + e); // TODO emit은 실패해도 됨.
            }
            if (actionEvents.isEmpty()) return;

            List<Point> points = new ArrayList<>(actionEvents.size());
            for (ActionEvent actionEvent : actionEvents) {
                Point point = this.toPoint(actionEvent);
                points.add(point);
            }
            writeApi.writePoints(points);
        } catch (Exception e) {
            log.error("failed to write points " + e);
        }
    }


    /**
     * 수집된 데이터를 기반으로 InfluxDB에 저장할 Point 객체를 생성
     */
    // TODO ObjectMapper로 ActionEvent에 매핑
    private Point toPoint(ActionEvent actionEvent) {
        Point point = Point.measurement(actionEventMeasurementKey)
                .addTag("eventType", actionEvent.getEventType().name())
                .addTag("actionGroupId", actionEvent.getActionGroupId().toString())
                .addTag("actionId", actionEvent.getActionId().toString())
                .addField("customerId", actionEvent.getCustomerId())
                .addField("eventTimestamp", actionEvent.getEventTimestamp())
                .addField("count", 1)
                .time(actionEvent.getRecordTimestamp(), WritePrecision.MS);

        if (actionEvent.getWaitTimeMs() != null) {
            point.addField("waitTimeMs", actionEvent.getWaitTimeMs());
        }

        return point;
    }
}