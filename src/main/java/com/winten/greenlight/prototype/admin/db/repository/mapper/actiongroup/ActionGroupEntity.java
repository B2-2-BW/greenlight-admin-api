package com.winten.greenlight.prototype.admin.db.repository.mapper.actiongroup;

import com.winten.greenlight.prototype.admin.support.dto.Hashable;
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
    private Integer maxActiveCustomers;
    private Boolean enabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}