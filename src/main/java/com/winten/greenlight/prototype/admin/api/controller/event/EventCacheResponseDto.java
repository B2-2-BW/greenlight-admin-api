package com.winten.greenlight.prototype.admin.api.controller.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventCacheResponseDto {
    private Integer refreshCount;
}