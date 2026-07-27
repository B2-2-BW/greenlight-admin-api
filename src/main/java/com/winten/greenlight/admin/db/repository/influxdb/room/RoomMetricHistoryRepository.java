package com.winten.greenlight.admin.db.repository.influxdb.room;

import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.winten.greenlight.admin.domain.queuestatistics.QueueStatisticsPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class RoomMetricHistoryRepository {
    private static final Pattern WINDOW_PATTERN = Pattern.compile("[1-9]\\d*[smhdw]");

    private final QueryApi queryApi;
    @Value("${influxdb.default-bucket}")
    private String bucket;

    public List<RoomMetricRecord> findByRoomIds(List<String> roomIds, Instant from, Instant to, String window) {
        String roomPredicate = fluxOrPredicate("r.room_id", roomIds);
        String safeWindow = fluxDurationLiteral(window);
        String flux = """
                gauges = from(bucket: %s)
                    |> range(start: %s, stop: %s)
                    |> filter(fn: (r) => r._measurement == "room_metric" and %s and (%s))
                    |> aggregateWindow(every: %s, fn: last, createEmpty: false)

                counts = from(bucket: %s)
                    |> range(start: %s, stop: %s)
                    |> filter(fn: (r) => r._measurement == "room_metric" and %s and (%s))
                    |> aggregateWindow(every: %s, fn: sum, createEmpty: false)

                union(tables: [gauges, counts])
                    |> group(columns: ["room_id"])
                    |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                    |> sort(columns: ["_time"])
                """.formatted(
                fluxStringLiteral(bucket), fluxTimeLiteral(from), fluxTimeLiteral(to), roomPredicate,
                fluxOrPredicate("r._field", List.of("room_capacity", "total_waiting", "total_active", "estimated_wait_time")), safeWindow,
                fluxStringLiteral(bucket), fluxTimeLiteral(from), fluxTimeLiteral(to), roomPredicate,
                fluxOrPredicate("r._field", List.of("waiting_count", "entered_count", "exited_count", "cancelled_count")), safeWindow);

        return queryApi.query(flux).stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::toRecord)
                .toList();
    }

    private static String fluxTimeLiteral(Instant time) {
        return java.util.Objects.requireNonNull(time, "time").toString();
    }

    private static String fluxDurationLiteral(String duration) {
        if (duration == null || !WINDOW_PATTERN.matcher(duration).matches()) {
            throw new IllegalArgumentException("Unsupported Flux duration: " + duration);
        }
        return duration;
    }

    private static String fluxOrPredicate(String column, List<String> values) {
        if (values.isEmpty()) {
            return "(false)";
        }
        return values.stream()
                .map(value -> column + " == " + fluxStringLiteral(value))
                .collect(java.util.stream.Collectors.joining(" or ", "(", ")"));
    }

    private static String fluxStringLiteral(String value) {
        StringBuilder literal = new StringBuilder("\"");
        for (char character : java.util.Objects.requireNonNull(value, "value").toCharArray()) {
            switch (character) {
                case '\\' -> literal.append("\\\\");
                case '\"' -> literal.append("\\\"");
                case '\n' -> literal.append("\\n");
                case '\r' -> literal.append("\\r");
                case '\t' -> literal.append("\\t");
                default -> {
                    if (Character.isISOControl(character)) {
                        literal.append("\\u%04x".formatted((int) character));
                    } else {
                        literal.append(character);
                    }
                }
            }
        }
        return literal.append('\"').toString();
    }

    private RoomMetricRecord toRecord(FluxRecord record) {
        return new RoomMetricRecord(
                (String) record.getValueByKey("room_id"),
                new QueueStatisticsPoint(
                        record.getTime(), asLong(record.getValueByKey("room_capacity")),
                        asLong(record.getValueByKey("total_waiting")), asLong(record.getValueByKey("total_active")),
                        asLong(record.getValueByKey("estimated_wait_time")), asLong(record.getValueByKey("waiting_count")),
                        asLong(record.getValueByKey("entered_count")), asLong(record.getValueByKey("exited_count")),
                        asLong(record.getValueByKey("cancelled_count"))
                )
        );
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    public record RoomMetricRecord(String roomId, QueueStatisticsPoint point) {
    }
}
