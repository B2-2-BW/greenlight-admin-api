package com.winten.greenlight.prototype.admin.domain.actionevent;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.winten.greenlight.prototype.admin.db.repository.influxdb.actionevent.ActionEventRepository;
import com.winten.greenlight.prototype.admin.domain.actionevent.dto.ActionEvent;
import com.winten.greenlight.prototype.admin.domain.actionevent.dto.ActionEventTraffic;
import com.winten.greenlight.prototype.admin.domain.actionevent.dto.ActionEventTrafficResponse;
import com.winten.greenlight.prototype.admin.domain.actionevent.dto.ActionEventTrafficSummary;
import com.winten.greenlight.prototype.admin.domain.actiongroup.ActionGroupQueue;
import com.winten.greenlight.prototype.admin.domain.actiongroup.ActionGroupService;
import com.winten.greenlight.prototype.admin.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActionEventService {
    private final ActionGroupService actionGroupService;
    private final ActionEventRepository actionEventRepository;

    public ActionEventTrafficResponse makeTrafficResponse(List<ActionEvent> actionEvents) {
        var trafficMap = this.getActionEventTrafficMap();

        for (ActionEvent actionEvent : actionEvents) {
            var actionGroupId = actionEvent.getActionGroupId();
            if (!trafficMap.containsKey(actionGroupId)) continue;
            ActionEventTraffic traffic = trafficMap.get(actionGroupId);
            var eventType = actionEvent.getEventType();
            if (eventType == WaitStatus.WAITING || eventType == WaitStatus.BYPASSED) {
                traffic.addRequest(1);
            } else if (eventType == WaitStatus.ENTERED) {
                traffic.addEntered(1);
            }
        }

        List<FluxTable> average = actionEventRepository.getCurrentTraffic10sAverage();
        for (FluxTable table : average) {
            for (FluxRecord record : table.getRecords()) {
                Map<String, Object> values = record.getValues();
                Object valueObj = record.getValue();
                double value;
                if (valueObj == null) {
                    value = 0.0;
                } else {
                    value = Double.parseDouble(valueObj.toString());
                }
                Long actionGroupId = Long.parseLong(values.get("actionGroupId").toString());
                ActionEventTraffic traffic = trafficMap.get(actionGroupId);
                var eventType = values.get("eventType").toString();
                if (WaitStatus.WAITING.name().equals(eventType) || WaitStatus.BYPASSED.name().equals(eventType)) {
                    traffic.setAverageRequestCount(value);
                } else if (WaitStatus.ENTERED.name().equals(eventType)) {
                    traffic.setAverageEnteredCount(value);
                }
            }
        }

        ActionEventTrafficSummary summary = this.makeSummary(trafficMap.values());
        Long sessionCount = actionGroupService.getSessionCount();
        summary.setSessionCount(sessionCount);
        return ActionEventTrafficResponse.builder()
                .detail(trafficMap)
                .summary(summary)
                .build();
    }

    private Map<Long, ActionEventTraffic> getActionEventTrafficMap() {
        CurrentUser currentUser = CurrentUser.builder()
                .userId("admin")
                .build();
        Map<Long, ActionEventTraffic> trafficDetail = new HashMap<>();
        List<ActionGroupQueue> queueSizes = actionGroupService.getActionGroupQueueStatus(currentUser);

        for (ActionGroupQueue queue : queueSizes) {
            var traffic = ActionEventTraffic.empty();
            traffic.setWaitingCount(queue.getWaitingSize());
            traffic.setEstimatedWaitTime(queue.getEstimatedWaitTime());
            traffic.setAverageActiveUserCount(queue.getActiveUserCount());
            trafficDetail.put(queue.getActionGroupId(), traffic);
        }
        return trafficDetail;
    }

    private ActionEventTrafficSummary makeSummary(Collection<ActionEventTraffic> trafficList) {
        var summary = ActionEventTrafficSummary.empty();
        for (ActionEventTraffic traffic : trafficList) {
            summary.addRequest(traffic.getRequestCount());
            summary.addWaiting(traffic.getWaitingCount());
            summary.addEntered(traffic.getEnteredCount());
            summary.addAverageRequest(traffic.getAverageRequestCount());
            summary.addAverageEntered(traffic.getAverageEnteredCount());
            summary.addEstimatedWaitTime(traffic.getEstimatedWaitTime());
        }
        summary.setEstimatedWaitTime(Math.round((float) summary.getEstimatedWaitTime() / trafficList.size()));
        return summary;
    }
}