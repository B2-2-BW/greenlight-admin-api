package com.winten.greenlight.admin.domain.systemstatus;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemStatusServiceTest {

    @Test
    void isolatesEachComponentFailureAsDown() throws Exception {
        var dataSource = mock(DataSource.class);
        var redisConnectionFactory = mock(RedisConnectionFactory.class);
        var schedulerStatusClient = mock(SchedulerStatusClient.class);

        when(dataSource.getConnection()).thenThrow(new SQLException("database unavailable"));
        when(redisConnectionFactory.getConnection()).thenThrow(new IllegalStateException("redis unavailable"));
        when(schedulerStatusClient.getSchedulers()).thenThrow(new IllegalStateException("scheduler unavailable"));

        var service = new SystemStatusService(dataSource, redisConnectionFactory, schedulerStatusClient);
        var result = service.getSystemStatus();

        assertThat(result.getDatabase().getStatus()).isEqualTo("DOWN");
        assertThat(result.getRedis().getStatus()).isEqualTo("DOWN");
        assertThat(result.getScheduler().getStatus()).isEqualTo("DOWN");
        assertThat(result.getScheduler().getSchedulers()).isEmpty();
    }
}
