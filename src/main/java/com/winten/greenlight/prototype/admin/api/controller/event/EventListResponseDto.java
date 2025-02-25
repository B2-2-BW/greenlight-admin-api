package com.winten.greenlight.prototype.admin.api.controller.event;

import com.winten.greenlight.prototype.admin.domain.event.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class EventListResponseDto {
    private List<EventResponseDto> events;

    public EventListResponseDto(List<Event> events) {
        this.events = events.stream()
                .map(EventResponseDto::new)
                .toList();
    }
}