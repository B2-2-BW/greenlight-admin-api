package com.winten.greenlight.prototype.admin.domain.event;

import com.winten.greenlight.prototype.admin.db.repository.redis.EventEntity;
import com.winten.greenlight.prototype.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Event extends AuditDto {
    private Long eventSeq;
    private String eventName;
    private String eventDescription;
    private String eventType;
    private String eventUrl;
    private Integer queueBackpressure;

    public Event(final EventEntity entity) {
        this.eventName = entity.getEventName();
        this.eventDescription = entity.getEventDescription();
        this.eventType = entity.getEventType();
        this.eventUrl = entity.getEventUrl();
        this.queueBackpressure = entity.getQueueBackpressure();
    }

    public EventEntity toEntity() {
        final EventEntity entity = new EventEntity();
        entity.setEventName(eventName);
        entity.setEventDescription(eventDescription);
        entity.setEventType(eventType);
        entity.setEventUrl(eventUrl);
        entity.setQueueBackpressure(queueBackpressure);
        return entity;
    }
}