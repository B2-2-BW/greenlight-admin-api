package com.winten.greenlight.admin.db.repository.influxdb.room;

import com.influxdb.client.QueryApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomMetricHistoryRepositoryTest {

    @Test
    void generatesStoragePushdownFriendlyIndependentPipelinesWithEscapedStaticPredicates() {
        QueryApi queryApi = mock(QueryApi.class);
        when(queryApi.query(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        RoomMetricHistoryRepository repository = new RoomMetricHistoryRepository(queryApi);
        ReflectionTestUtils.setField(repository, "bucket", "queue\\\"bucket\nname");

        repository.findByRoomIds(
                List.of("room\\\"one\nnext", "room-two"),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-01T01:00:00Z"),
                "1m"
        );

        ArgumentCaptor<String> fluxCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryApi).query(fluxCaptor.capture());
        String flux = fluxCaptor.getValue();
        assertThat(flux)
                .doesNotContain("params.", "import \"strings\"", "contains(", "base =")
                .contains("from(bucket: \"queue\\\\\\\"bucket\\nname\")")
                .contains("range(start: 2026-07-01T00:00:00Z, stop: 2026-07-01T01:00:00Z)")
                .contains("(r.room_id == \"room\\\\\\\"one\\nnext\" or r.room_id == \"room-two\")")
                .contains("(r._field == \"room_capacity\" or r._field == \"total_waiting\" or r._field == \"total_active\" or r._field == \"estimated_wait_time\")")
                .contains("(r._field == \"waiting_count\" or r._field == \"entered_count\" or r._field == \"exited_count\" or r._field == \"cancelled_count\")")
                .contains("gauges = from(bucket:")
                .contains("counts = from(bucket:")
                .contains("aggregateWindow(every: 1m, fn: last, createEmpty: false)")
                .contains("aggregateWindow(every: 1m, fn: sum, createEmpty: false)");
        assertThat(countOccurrences(flux, "from(bucket:")).isEqualTo(2);

        String gauges = flux.substring(flux.indexOf("gauges ="), flux.indexOf("counts ="));
        String counts = flux.substring(flux.indexOf("counts ="), flux.indexOf("union(tables:"));
        assertThat(gauges)
                .contains("r._field == \"room_capacity\"")
                .doesNotContain("r._field == \"waiting_count\"");
        assertThat(counts)
                .contains("r._field == \"waiting_count\"")
                .doesNotContain("r._field == \"room_capacity\"");
    }

    @Test
    void concurrentGaugesSumLastSnapshotsAcrossRoomsThenKeepWindowSeries() {
        QueryApi queryApi = mock(QueryApi.class);
        when(queryApi.query(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        RoomMetricHistoryRepository repository = new RoomMetricHistoryRepository(queryApi);
        ReflectionTestUtils.setField(repository, "bucket", "queue-bucket");

        repository.findConcurrentGauges(
                List.of("room-one", "room-two"),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-01T01:00:00Z"),
                "1m"
        );

        ArgumentCaptor<String> fluxCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryApi).query(fluxCaptor.capture());
        String flux = fluxCaptor.getValue();
        assertThat(flux)
                .contains("from(bucket: \"queue-bucket\")")
                .contains("(r.room_id == \"room-one\" or r.room_id == \"room-two\")")
                .contains("(r._field == \"total_waiting\" or r._field == \"total_active\")")
                .contains("aggregateWindow(every: 1m, fn: last, createEmpty: false)")
                .contains("group(columns: [\"_field\"])")
                .contains("aggregateWindow(every: 1m, fn: sum, createEmpty: false)")
                .doesNotContain("waiting_count")
                .doesNotContain("room_capacity");
        assertThat(countOccurrences(flux, "aggregateWindow(")).isEqualTo(2);
    }

    private static int countOccurrences(String value, String target) {
        return value.split(java.util.regex.Pattern.quote(target), -1).length - 1;
    }
}
