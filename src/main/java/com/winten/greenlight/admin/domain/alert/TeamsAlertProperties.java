package com.winten.greenlight.admin.domain.alert;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "teams.alert")
public class TeamsAlertProperties {
    private String url;
    private String token;
    private boolean enabled = true;
}
