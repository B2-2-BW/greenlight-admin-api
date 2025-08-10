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

    public Object getCurrentTraffic() {
        CurrentUser currentUser = CurrentUser.builder()
                .userId("admin")
                .build();
        return this.getCurrentTrafficDetail(currentUser);
    }

    public Map<Long, ActionEventTraffic> getCurrentTrafficDetail(CurrentUser currentUser) {

        Map<Long, ActionEventTraffic> result = new HashMap<>();
        List<ActionGroupQueue> queueSizes = actionGroupService.getAllWaitingQueueSize(currentUser);

        for (ActionGroupQueue queue : queueSizes) {
            var traffic = ActionEventTraffic.empty();
            traffic.addWaiting(queue.getSize());
            result.put(queue.getActionGroupId(), traffic);
        }

        List<FluxTable> tables = actionEventRepository.getCurrentTrafficDetail();
        for (FluxTable table : tables) {
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
                    traffic.addEntered(value);
                } else if (requestEventType.contains(eventType)) {
                    traffic.addRequest(value);
                }
            }
        }
        var total = sumOf(result.values());
        result.put(0L, total);
        return result;
    }

    private ActionEventTraffic sumOf(Collection<ActionEventTraffic> trafficList) {
        var total = ActionEventTraffic.empty();
        for (ActionEventTraffic traffic : trafficList) {
            total.addRequest(traffic.getRequestCount());
            total.addWaiting(traffic.getWaitingCount());
            total.addEntered(traffic.getEnteredCount());
        }
        return total;
    }

    public Object getCurrentTrafficSummary() {
        List<FluxTable> tables = actionEventRepository.get5minCustomerCount();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Map<String, Object> values = record.getValues();
                System.out.println(values);
            }
        }
        return new HashMap<>();
    }
}