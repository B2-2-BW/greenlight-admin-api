package com.winten.greenlight.admin.domain.cache;

import com.winten.greenlight.admin.domain.alert.AlertPolicyService;
import com.winten.greenlight.admin.domain.room.RoomService;
import com.winten.greenlight.admin.domain.site.SiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationalCacheSync {
    private final SiteService siteService;
    private final RoomService roomService;
    private final AlertPolicyService alertPolicyService;
    private final RedisTemplate<String, String> redisTemplate;

    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private volatile boolean redisUp = true;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        redisUp = ping();
        if (redisUp) {
            sync();
        }
    }

    @Scheduled(fixedDelayString = "${operational-cache.sync-ms:300000}")
    public void syncOnSchedule() {
        sync();
    }

    @Scheduled(fixedDelayString = "${operational-cache.probe-ms:15000}")
    public void probeRedis() {
        boolean up = ping();
        if (up && !redisUp) {
            sync();
        }
        redisUp = up;
    }

    void sync() {
        if (!inFlight.compareAndSet(false, true)) {
            return;
        }
        try {
            siteService.reloadAllSitesForSystem();
            roomService.reloadAllRoomsForSystem();
            alertPolicyService.reloadAllForSystem();
            log.info("Operational cache synced");
        } catch (Exception exception) {
            log.error("Operational cache sync failed", exception);
        } finally {
            inFlight.set(false);
        }
    }

    boolean ping() {
        try {
            var factory = redisTemplate.getConnectionFactory();
            if (factory == null) {
                return false;
            }
            try (var connection = factory.getConnection()) {
                String pong = connection.ping();
                return pong != null && pong.equalsIgnoreCase("PONG");
            }
        } catch (Exception exception) {
            log.warn("Redis ping failed. message={}", exception.getMessage());
            return false;
        }
    }
}
