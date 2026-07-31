package com.winten.greenlight.admin.domain.systemstatus;

import com.winten.greenlight.admin.api.controller.systemstatus.SystemStatusResponse;
import com.winten.greenlight.admin.api.controller.systemstatus.SystemStatusResponse.ComponentStatus;
import com.winten.greenlight.admin.api.controller.systemstatus.SystemStatusResponse.SchedulerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemStatusService {
    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final SchedulerStatusClient schedulerStatusClient;

    public SystemStatusResponse getSystemStatus() {
        return SystemStatusResponse.builder()
                .database(checkDatabase())
                .redis(checkRedis())
                .scheduler(checkScheduler())
                .build();
    }

    private ComponentStatus checkDatabase() {
        try (
                var connection = dataSource.getConnection();
                var statement = connection.createStatement()
        ) {
            statement.setQueryTimeout(2);
            try (var result = statement.executeQuery("SELECT 1")) {
                if (result.next()) {
                    return component(UP);
                }
            }
        } catch (Exception exception) {
            log.debug("Database liveness check failed: {}", exception.getMessage());
        }
        return component(DOWN);
    }

    private ComponentStatus checkRedis() {
        try (var connection = redisConnectionFactory.getConnection()) {
            if ("PONG".equalsIgnoreCase(connection.ping())) {
                return component(UP);
            }
        } catch (Exception exception) {
            log.debug("Redis liveness check failed: {}", exception.getMessage());
        }
        return component(DOWN);
    }

    private SchedulerStatus checkScheduler() {
        try {
            return SchedulerStatus.builder()
                    .status(UP)
                    .schedulers(schedulerStatusClient.getSchedulers())
                    .build();
        } catch (Exception exception) {
            log.debug("Scheduler liveness check failed: {}", exception.getMessage());
            return SchedulerStatus.builder()
                    .status(DOWN)
                    .schedulers(java.util.List.of())
                    .build();
        }
    }

    private ComponentStatus component(String status) {
        return ComponentStatus.builder().status(status).build();
    }
}
