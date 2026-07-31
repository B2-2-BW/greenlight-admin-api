package com.winten.greenlight.admin.api.controller.systemstatus;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SystemStatusResponse {
    private ComponentStatus database;
    private ComponentStatus redis;
    private SchedulerStatus scheduler;

    @Getter
    @Builder
    public static class ComponentStatus {
        private String status;
    }

    @Getter
    @Builder
    public static class SchedulerStatus {
        private String status;
        private List<SchedulerItem> schedulers;
    }

    @Getter
    @Builder
    public static class SchedulerItem {
        private String schedulerCode;
        private String status;
        private String name;
        private String description;
    }
}
