package com.winten.greenlight.admin.api.controller.site;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SiteInfoRequestTest {
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void tracksSiteManagementFieldPresenceEvenWhenValueIsNull() throws Exception {
        var request = jsonMapper.readValue(
                """
                {
                  "siteName": null,
                  "queueEnabled": false
                }
                """,
                SiteInfoRequest.class
        );

        assertThat(request.hasSiteManagementFields()).isTrue();
        assertThat(request.isQueueEnabledPresent()).isTrue();
        assertThat(request.getQueueEnabled()).isFalse();
    }

    @Test
    void queueOnlyPayloadDoesNotClaimSiteManagementFields() throws Exception {
        var request = jsonMapper.readValue("{\"queueEnabled\":true}", SiteInfoRequest.class);

        assertThat(request.hasSiteManagementFields()).isFalse();
        assertThat(request.isQueueEnabledPresent()).isTrue();
    }

    @Test
    void clientsCannotOverrideServerSidePresenceTracking() throws Exception {
        var request = jsonMapper.readValue(
                """
                {
                  "siteName": "변경 이름",
                  "siteManagementFieldsPresent": false,
                  "queueEnabled": false,
                  "queueEnabledPresent": false
                }
                """,
                SiteInfoRequest.class
        );

        assertThat(request.hasSiteManagementFields()).isTrue();
        assertThat(request.isQueueEnabledPresent()).isTrue();
    }
}
