package com.winten.greenlight.admin.domain.alert;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamsMessage {
    private String referer;
    private String content;
    private String notificationType;
    private List<String> targets;
    private String sentAt;
}