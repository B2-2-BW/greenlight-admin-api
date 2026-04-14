package com.winten.greenlight.admin.db.repository.mapper.room;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.winten.greenlight.admin.domain.action.DefaultRuleType;
import com.winten.greenlight.admin.domain.room.RoomEnvironment;
import com.winten.greenlight.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonFilter("roomFilter")
public class RoomEntity extends AuditDto {
    private String roomId;
    private String siteId;
    private RoomEnvironment roomEnvironment;
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Integer capacity;
    private Boolean enabled;
    private String defaultDestinationUrl;
    private DefaultRuleType defaultRuleType;
    private String adImageUrl;
    private List<RoomRuleEntity> roomRules;
}