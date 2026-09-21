package com.winten.greenlight.admin.domain.alert;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertCatalogTest {
    @Test
    void unknownAlertnameMapsToInfra() {
        assertThat(AlertCatalog.subscriptionKey("QUEUE_WAIT")).isEqualTo("QUEUE_WAIT");
        assertThat(AlertCatalog.subscriptionKey("SITE_DISABLED")).isEqualTo("QUEUE_DISABLED");
        assertThat(AlertCatalog.subscriptionKey("QUEUE_DISABLED")).isEqualTo("QUEUE_DISABLED");
        assertThat(AlertCatalog.subscriptionKey("ContainerDown")).isEqualTo("INFRA");
        assertThat(AlertCatalog.subscriptionKey("SCHEDULER_FAILED")).isEqualTo("INFRA");
        assertThat(AlertCatalog.subscriptionKey("SCHEDULER_STOPPED")).isEqualTo("INFRA");
        assertThat(AlertCatalog.subscriptionKey(null)).isEqualTo("INFRA");
    }

    @Test
    void catalogKeepsSiteAlertsAndSingleInfraSwitch() {
        assertThat(AlertCatalog.catalog(false))
                .containsExactly(AlertCatalog.QUEUE_WAIT, AlertCatalog.QUEUE_DISABLED, AlertCatalog.SITE_MAINTENANCE);
        assertThat(AlertCatalog.catalog(true)).extracting(AlertCatalog::name)
                .containsExactly("QUEUE_WAIT", "QUEUE_DISABLED", "SITE_MAINTENANCE", "INFRA")
                .doesNotContain("ACTIVE_USERS", "VISITOR_SURGE", "SCHEDULER_FAILED", "SCHEDULER_STOPPED");
    }
}
