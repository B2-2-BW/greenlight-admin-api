package com.winten.greenlight.prototype.admin.domain.actionevent;

import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionEventListener implements StreamListener<String, MapRecord<String, String, String>> {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final String consumerGroup = "admin-action-event-processor-group";
    // private final String consumerName = "greenlight:" + UUID.randomUUID();
    private final ActionEventProcessor actionEventProcessor;

    /**
     * 애플리케이션 시작 시 Consumer Group이 없으면 생성합니다.
     */
    @PostConstruct
    public void prepareConsumerGroup() {
        try {
            // XGROUP CREATE my-stream my-group $ MKSTREAM
            redisTemplate.opsForStream().createGroup(keyBuilder.actionEventStream(), ReadOffset.latest(), consumerGroup);
        } catch (RedisSystemException e) {
            if (e.getCause() != null && e.getCause().getMessage().contains("BUSYGROUP")) {
                log.info("Consumer group '{}' already exists.", consumerGroup);
            } else {
                log.error("Error creating Redis consumer group", e);
                throw e;
            }
        }
    }

    @EventListener(ApplicationStartedEvent.class)
    public void startListening() {
        container.receive(
                Consumer.from(consumerGroup, UUID.randomUUID().toString()),
                StreamOffset.create(keyBuilder.actionEventStream(), ReadOffset.lastConsumed()),
                this
        );
        container.start();
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            // 처리 로직을 별도 컴포넌트에 위임 (재시도 포함)
            actionEventProcessor.process(message.getValue());

            // 성공 시 ACK
            redisTemplate.opsForStream().acknowledge(keyBuilder.actionEventStream(), consumerGroup, message.getId());
            log.error("Message processed and acknowledged. ID: {}", message.getId());

        } catch (Exception e) {
            // @Retryable에서 모든 재시도 실패 후 예외가 여기까지 전달됨
            // @Recover 메서드가 처리했으므로 여기서는 추가 작업 없이 로그만 남기거나,
            // 별도의 처리가 필요하다면 여기에 구현합니다.
            // 중요: 실패 시 ACK를 호출하지 않아야 합니다.
            log.error("Failed to process message ID: {}. It might have been moved to DLQ.", message.getId(), e);
        }
    }
}