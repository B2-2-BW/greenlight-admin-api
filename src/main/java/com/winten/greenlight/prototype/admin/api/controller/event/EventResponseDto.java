package com.winten.greenlight.prototype.admin.api.controller.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.prototype.admin.domain.event.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventResponseDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long eventSeq;
    private String eventName;
    private String eventDescription;
    private String eventType;
    private String eventUrl;
    private Integer queueBackpressure;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String createdBy;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected LocalDateTime createdAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String updatedBy;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected LocalDateTime updatedAt;

    public EventResponseDto(Event event) {
        this.eventSeq = event.getEventSeq();
        this.eventName = event.getEventName();
        this.eventDescription = event.getEventDescription();
        this.eventType = event.getEventType();
        this.eventUrl = event.getEventUrl();
        this.queueBackpressure = event.getQueueBackpressure();
        this.eventStartTime = event.getEventStartTime();
        this.eventEndTime = event.getEventEndTime();
        this.createdBy = event.getCreatedBy();
        this.createdAt = event.getCreatedAt();
        this.updatedBy = event.getUpdatedBy();
        this.updatedAt = event.getUpdatedAt();
    }
}