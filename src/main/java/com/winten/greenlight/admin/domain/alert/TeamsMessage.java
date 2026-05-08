package com.winten.greenlight.admin.domain.alert;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamsMessage {
    private String referer;
    private String content;
    private String notificationType;
}