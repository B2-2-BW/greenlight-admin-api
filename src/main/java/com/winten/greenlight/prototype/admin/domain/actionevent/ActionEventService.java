package com.winten.greenlight.prototype.admin.domain.actionevent;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.winten.greenlight.prototype.admin.db.repository.influxdb.actionevent.ActionEventRepository;
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
    private final ActionEventRepository actionEventRepository;
    private final ActionGroupService actionGroupService;

    private final List<String> requestEventType = List.of(WaitStatus.WAITING.name(), WaitStatus.BYPASSED.name());

    public ActionEventTrafficResponse getCurrentTraffic() {
        CurrentUser currentUser = CurrentUser.builder()
                .userId("admin")
                .build();
        Map<Long, ActionEventTraffic> result = new HashMap<>();
        List<ActionGroupQueue> queueSizes = actionGroupService.getActionGroupQueueStatus(currentUser);

        for (ActionGroupQueue queue : queueSizes) {
            var traffic = ActionEventTraffic.empty();
            traffic.setWaitingCount(queue.getWaitingSize());
            traffic.setConcurrentUser(queue.getSessionSize());
            traffic.setEstimatedWaitTime(queue.getEstimatedWaitTime());
            result.put(queue.getActionGroupId(), traffic);
        }

        List<FluxTable> details = actionEventRepository.getCurrentTrafficDetail();
        for (FluxTable table : details) {
            for (FluxRecord record : table.getRecords()) {
                Map<String, Object> values = record.getValues();
                Object valueObj = record.getValue();
                int value;
                if (valueObj == null) {
                    value = 0;
                } else {
                    value = Integer.parseInt(valueObj.toString());
                }
                Long actionGroupId = Long.parseLong(values.get("actionGroupId").toString());
                var traffic = result.get(actionGroupId);
                var eventType = values.get("eventType").toString();
                if (WaitStatus.ENTERED.name().equals(eventType)) {
                    traffic.setEnteredCount(value);
                } else if (requestEventType.contains(eventType)) {
                    traffic.setRequestCount(value);
                }
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
                var traffic = result.get(actionGroupId);
                var eventType = values.get("eventType").toString();
                if (WaitStatus.ENTERED.name().equals(eventType)) {
                    traffic.setEnteredAverageCount(value);
                } else if (requestEventType.contains(eventType)) {
                    traffic.setRequestAverageCount(value);
                }
            }
        }

        ActionEventTrafficSummary summary = makeSummary(result.values());
        Long sessionCount = actionGroupService.getSessionCount();
        summary.setSessionCount(sessionCount);
        return ActionEventTrafficResponse.builder()
                .detail(result)
                .summary(summary)
                .build();
    }

    private ActionEventTrafficSummary makeSummary(Collection<ActionEventTraffic> trafficList) {
        var summary = ActionEventTrafficSummary.empty();
        for (ActionEventTraffic traffic : trafficList) {
            summary.addRequest(traffic.getRequestCount());
            summary.addWaiting(traffic.getWaitingCount());
            summary.addEntered(traffic.getEnteredCount());
            summary.addRequestAverage(traffic.getRequestAverageCount());
            summary.addEnteredAverage(traffic.getEnteredAverageCount());
            summary.addConcurrentUser(traffic.getConcurrentUser());
            summary.addEstimatedWaitTime(traffic.getEstimatedWaitTime());
        }
        summary.setEstimatedWaitTime(Math.round((float) summary.getEstimatedWaitTime() / trafficList.size()));
        return summary;
    }
}