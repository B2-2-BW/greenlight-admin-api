package com.winten.greenlight.admin.db.repository.mapper.room;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.winten.greenlight.admin.domain.action.MatchOperator;
import com.winten.greenlight.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonFilter("roomRuleFilter")
public class RoomRuleEntity extends AuditDto {
    private String roomId;
    private Long ruleSeq;
    private String siteId;
    private String value;
    private MatchOperator matchOperator;
    private String description;
}