package com.winten.greenlight.prototype.admin.db.repository.redis;

import lombok.Data;

import java.io.Serializable;

@Data
public class EventEntity implements Serializable {
    private String eventName;
    private String eventDescription;
    private String eventType;
    private String eventUrl;
    private Integer queueBackpressure;
}