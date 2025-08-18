package com.winten.greenlight.prototype.admin.domain.actionevent;

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

    public ActionEventTrafficResponse makeTrafficResponse(List<ActionEvent> actionEvents) {
        var trafficDetail = this.getTrafficDetail();

        for (ActionEvent actionEvent : actionEvents) {
            var actionGroupId = actionEvent.getActionGroupId();
            if (!trafficDetail.containsKey(actionGroupId)) continue;
            ActionEventTraffic traffic = trafficDetail.get(actionGroupId);
            var eventType = actionEvent.getEventType();
            if (eventType == WaitStatus.WAITING || eventType == WaitStatus.BYPASSED) {
                traffic.addRequest(1);
            } else if (eventType == WaitStatus.ENTERED) {
                traffic.addEntered(1);
            }
        }

        ActionEventTrafficSummary summary = this.makeSummary(trafficDetail.values());
        Long sessionCount = actionGroupService.getSessionCount();
        summary.setSessionCount(sessionCount);
        return ActionEventTrafficResponse.builder()
                .detail(trafficDetail)
                .summary(summary)
                .build();
    }

    private Map<Long, ActionEventTraffic> getTrafficDetail() {
        CurrentUser currentUser = CurrentUser.builder()
                .userId("admin")
                .build();
        Map<Long, ActionEventTraffic> trafficDetail = new HashMap<>();
        List<ActionGroupQueue> queueSizes = actionGroupService.getActionGroupQueueStatus(currentUser);

        for (ActionGroupQueue queue : queueSizes) {
            var traffic = ActionEventTraffic.empty();
            traffic.setWaitingCount(queue.getWaitingSize());
            traffic.setEstimatedWaitTime(queue.getEstimatedWaitTime());
            traffic.setAverageActiveUserCount(queue.getActiveUserCount() / 3.0);
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