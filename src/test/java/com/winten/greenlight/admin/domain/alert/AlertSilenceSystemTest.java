package com.winten.greenlight.admin.domain.alert;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertSilenceSystemTest {
    @Test
    void mapsAlertnamesOntoSystems() {
        assertThat(AlertSilenceSystem.ofAlertname("QUEUE_WAIT")).isEqualTo(AlertSilenceSystem.QUEUE);
        assertThat(AlertSilenceSystem.ofAlertname("SCHEDULER_FAILED")).isEqualTo(AlertSilenceSystem.SCHEDULER);
        assertThat(AlertSilenceSystem.ofAlertname("SCHEDULER_STOPPED")).isEqualTo(AlertSilenceSystem.SCHEDULER);
        assertThat(AlertSilenceSystem.ofAlertname("ContainerDown")).isEqualTo(AlertSilenceSystem.DOCKER);
        assertThat(AlertSilenceSystem.ofAlertname("RedisClusterDown")).isEqualTo(AlertSilenceSystem.REDIS);
        assertThat(AlertSilenceSystem.ofAlertname("HostLowDiskSpace")).isEqualTo(AlertSilenceSystem.HOST);
        assertThat(AlertSilenceSystem.ofAlertname("PrometheusTargetDown")).isEqualTo(AlertSilenceSystem.TARGET);
        assertThat(AlertSilenceSystem.ofAlertname("QUEUE_DISABLED")).isNull();
        assertThat(AlertSilenceSystem.ofAlertname("SITE_MAINTENANCE")).isNull();
        assertThat(AlertSilenceSystem.fromCode("redis")).isEqualTo(AlertSilenceSystem.REDIS);
        assertThat(AlertSilenceSystem.fromCode("MAIL")).isNull();
    }
}
