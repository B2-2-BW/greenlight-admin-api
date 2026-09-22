package com.winten.greenlight.admin.domain.systemstatus;

import com.winten.greenlight.admin.api.controller.systemstatus.SystemStatusResponse.SchedulerItem;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerRunningStatusStore {
    private static final List<String> SCHEDULER_CODES = List.of(
            "WAITING_TO_READY",
            "METRIC",
            "REMOVE_EXPIRED"
    );
    private static final String RUNNING = "RUNNING";

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;

    public void save(String schedulerCode, boolean enabled) {
        try {
            redisTemplate.opsForValue().set(
                    redisKeyBuilder.schedulerEnabled(schedulerCode),
                    Boolean.toString(enabled)
            );
        } catch (Exception exception) {
            log.error("Failed to store scheduler running status. code={} enabled={}", schedulerCode, enabled, exception);
        }
    }

    public void saveAll(List<SchedulerItem> schedulers) {
        if (schedulers == null) {
            return;
        }
        for (SchedulerItem scheduler : schedulers) {
            save(scheduler.getSchedulerCode(), RUNNING.equals(scheduler.getStatus()));
        }
    }

    public void saveAllDisabled() {
        for (String schedulerCode : SCHEDULER_CODES) {
            save(schedulerCode, false);
        }
    }
}
