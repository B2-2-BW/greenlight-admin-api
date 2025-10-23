package com.winten.greenlight.admin.db.repository.mapper.actiongroup;

import com.winten.greenlight.admin.support.dto.Hashable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionGroupEntity implements Hashable {
    private Long id;
    private String ownerId;
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Boolean enabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}