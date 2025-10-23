package com.winten.greenlight.admin.db.repository.influxdb.actionevent;

import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ActionEventRepository {
    private final QueryApi queryApi;

    public List<FluxTable> getCurrentTrafficDetail() {
        String flux = """
                import "timezone"
                option location = timezone.fixed(offset: 9h)
                
                from(bucket: "action_event")
                    |> range(start: -2s)
                    |> filter(fn: (r) => r._measurement == "greenlight_action_event")
                    |> filter(fn: (r) => r._field == "count")
                    |> group(columns: ["actionGroupId", "eventType"])
                    |> aggregateWindow(every: 2s, fn: sum, createEmpty: false)
                    |> yield(name: "waiting_count")
                """;
        return queryApi.query(flux);
    }

    public List<FluxTable> getCurrentTraffic10sAverage() {
        String flux = """
                import "timezone"
                option location = timezone.fixed(offset: 9h)
                
                from(bucket: "action_event")
                    |> range(start: -10s)
                    |> filter(fn: (r) => r._measurement == "greenlight_action_event")
                    |> filter(fn: (r) => r._field == "count")
                    |> group(columns: ["actionGroupId", "eventType"])
                    |> aggregateWindow(every: 10s, fn: sum, createEmpty: false)
                    |> map(fn: (r) => ({ r with _value: float(v: r._value) / 10.0 }))
                    |> yield(name: "waiting_count")
                """;
        return queryApi.query(flux);
    }


    /**
     * 5분 평균 진입 고객 수 계산 쿼리
     */
    public List<FluxTable> get5minCustomerCount() {
        String flux = """
                import "timezone"
                option location = timezone.fixed(offset: 9h)
                
                from(bucket: "action_event")
                    |> range(start: -5m)
                    |> filter(fn: (r) => r._measurement == "greenlight_action_event")
                    |> filter(fn: (r) => r.eventType == "WAITING" or r.eventType == "BYPASSED")
                    |> filter(fn: (r) => r._field == "count")
                    |> aggregateWindow(every: 5m, fn: sum, createEmpty: false)
                    |> yield(name: "waiting_count")
                """;
        return queryApi.query(flux);
    }
}