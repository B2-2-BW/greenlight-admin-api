package com.winten.greenlight.admin.domain.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSilence {
    private Long silenceId;
    private String systemCode;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime cancelledAt;
    private String comment;
    private String createdBy;
    private LocalDateTime createdAt;
    private String createdIp;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String updatedIp;
    private String status;
}
