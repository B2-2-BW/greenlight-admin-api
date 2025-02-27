package com.winten.greenlight.prototype.admin.api.controller.event;

import com.winten.greenlight.prototype.admin.domain.event.Event;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequestDto {
    private String eventName;
    private String eventDescription;
    private String eventType;
    private String eventUrl;
    private Integer queueBackpressure;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;
    private String createdBy;
    private String updatedBy;

    public Event toEvent() {
        Event event = new Event();
        event.setEventName(eventName);
        event.setEventDescription(eventDescription);
        event.setEventType(eventType);
        event.setEventUrl(eventUrl);
        event.setQueueBackpressure(queueBackpressure);
        event.setEventStartTime(eventStartTime);
        event.setEventEndTime(eventEndTime);
        event.setCreatedBy(createdBy);
        event.setUpdatedBy(updatedBy);
        return event;
    }
}