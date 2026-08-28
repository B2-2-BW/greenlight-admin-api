package com.winten.greenlight.admin.domain.queuestatistics;

import com.winten.greenlight.admin.db.repository.influxdb.room.RoomMetricHistoryRepository;
import com.winten.greenlight.admin.domain.room.Room;
import com.winten.greenlight.admin.domain.room.RoomEnvironment;
import com.winten.greenlight.admin.domain.room.RoomService;
import com.winten.greenlight.admin.support.error.CoreException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueueStatisticsServiceTest {
    private final Instant from = Instant.parse("2026-07-01T00:00:00Z");
    private final Instant to = from.plus(2, ChronoUnit.HOURS);

    @Test
    void rejectsUnauthorizedOrDevRoomBeforeInfluxQuery() {
        RoomService roomService = mock(RoomService.class);
        RoomMetricHistoryRepository repository = mock(RoomMetricHistoryRepository.class);
        QueueStatisticsService service = new QueueStatisticsService(roomService, repository);
        when(roomService.getRoomListFiltered(any(Room.class))).thenReturn(List.of(
                Room.builder().roomId("live-room").roomEnvironment(RoomEnvironment.LIVE).build()
        ));

        assertThatThrownBy(() -> service.getStatistics(from, to, List.of("other-site-or-dev-room")))
                .isInstanceOf(CoreException.class);

        var roomFilter = org.mockito.ArgumentCaptor.forClass(Room.class);
        verify(roomService).getRoomListFiltered(roomFilter.capture());
        assertThat(roomFilter.getValue().getRoomEnvironment()).isEqualTo(RoomEnvironment.LIVE);
        verifyNoInteractions(repository);
    }

    @Test
    void selectsAllAccessibleLiveRoomsWhenRoomIdsAreOmitted() {
        RoomService roomService = mock(RoomService.class);
        RoomMetricHistoryRepository repository = mock(RoomMetricHistoryRepository.class);
        QueueStatisticsService service = new QueueStatisticsService(roomService, repository);
        Room first = Room.builder().roomId("live-1").name("One").roomEnvironment(RoomEnvironment.LIVE).build();
        Room second = Room.builder().roomId("live-2").name("Two").roomEnvironment(RoomEnvironment.LIVE).build();
        when(roomService.getRoomListFiltered(any(Room.class))).thenReturn(List.of(first, second));
        when(repository.findByRoomIds(List.of("live-1", "live-2"), from, to, "1m")).thenReturn(List.of());
        when(repository.findConcurrentGauges(List.of("live-1", "live-2"), from, to, "1m")).thenReturn(List.of());

        QueueStatistics result = service.getStatistics(from, to, null);

        assertThat(result.availableRooms()).extracting(QueueStatisticsRoomOption::roomId).containsExactly("live-1", "live-2");
        assertThat(result.series()).extracting(QueueStatisticsRoom::roomId).containsExactly("live-1", "live-2");
        assertThat(result.series()).allSatisfy(series -> assertThat(series.points()).isEmpty());
        assertThat(result.maxWaiting()).isZero();
        assertThat(result.maxActive()).isZero();
    }

    @Test
    void validatesRangeAndUsesExpectedWindow() {
        RoomService roomService = mock(RoomService.class);
        RoomMetricHistoryRepository repository = mock(RoomMetricHistoryRepository.class);
        QueueStatisticsService service = new QueueStatisticsService(roomService, repository);
        assertThatThrownBy(() -> service.getStatistics(from, from, null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.getStatistics(from.minus(8, ChronoUnit.DAYS), from, null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.getStatistics(from, from.plus(2, ChronoUnit.DAYS), null, "1m"))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.getStatistics(from, from.plus(8, ChronoUnit.DAYS), null, "10m"))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.getStatistics(from, from.plus(16, ChronoUnit.DAYS), null, "1h"))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.getStatistics(from, to, null, "5m")).isInstanceOf(CoreException.class);
        assertThat(QueueStatisticsService.selectWindow(java.time.Duration.ofHours(6))).isEqualTo("1m");
        assertThat(QueueStatisticsService.selectWindow(java.time.Duration.ofHours(7))).isEqualTo("5m");
        assertThat(QueueStatisticsService.selectWindow(java.time.Duration.ofHours(25))).isEqualTo("30m");
        verifyNoInteractions(roomService, repository);
    }

    @Test
    void mapsInfluxPointsIntoTheRequestedRoomSeries() {
        RoomService roomService = mock(RoomService.class);
        RoomMetricHistoryRepository repository = mock(RoomMetricHistoryRepository.class);
        QueueStatisticsService service = new QueueStatisticsService(roomService, repository);
        Room room = Room.builder()
                .roomId("live-1")
                .name("One")
                .description("상품 대기열")
                .enabled(false)
                .roomEnvironment(RoomEnvironment.LIVE)
                .build();
        QueueStatisticsPoint point = new QueueStatisticsPoint(from, 100L, 10L, 30L, 45L, 5L, 4L, 3L, 1L);
        when(roomService.getRoomListFiltered(any(Room.class))).thenReturn(List.of(room));
        Instant exportTo = from.plus(10, ChronoUnit.DAYS);
        when(repository.findByRoomIds(List.of("live-1"), from, exportTo, "1h"))
                .thenReturn(List.of(new RoomMetricHistoryRepository.RoomMetricRecord("live-1", point)));
        when(repository.findConcurrentGauges(List.of("live-1"), from, exportTo, "1h"))
                .thenReturn(List.of(new RoomMetricHistoryRepository.ConcurrentGaugeRecord(from, 10L, 30L)));

        QueueStatistics result = service.getStatistics(from, exportTo, List.of("live-1"), "1h");

        assertThat(result.availableRooms()).singleElement().satisfies(option -> {
            assertThat(option.description()).isEqualTo("상품 대기열");
            assertThat(option.enabled()).isFalse();
        });
        assertThat(result.series()).singleElement().satisfies(series -> {
            assertThat(series.name()).isEqualTo("One");
            assertThat(series.enabled()).isFalse();
            assertThat(series.points()).containsExactly(point);
        });
        assertThat(result.maxWaiting()).isEqualTo(10L);
        assertThat(result.maxActive()).isEqualTo(30L);
    }

    @Test
    void usesConcurrentSumPeaksInsteadOfSummedRoomMaxes() {
        RoomService roomService = mock(RoomService.class);
        RoomMetricHistoryRepository repository = mock(RoomMetricHistoryRepository.class);
        QueueStatisticsService service = new QueueStatisticsService(roomService, repository);
        Room first = Room.builder().roomId("live-1").name("One").roomEnvironment(RoomEnvironment.LIVE).build();
        Room second = Room.builder().roomId("live-2").name("Two").roomEnvironment(RoomEnvironment.LIVE).build();
        Instant later = from.plus(1, ChronoUnit.HOURS);
        when(roomService.getRoomListFiltered(any(Room.class))).thenReturn(List.of(first, second));
        when(repository.findByRoomIds(List.of("live-1", "live-2"), from, to, "1m")).thenReturn(List.of(
                new RoomMetricHistoryRepository.RoomMetricRecord("live-1",
                        new QueueStatisticsPoint(from, 100L, 10L, 1L, 0L, 0L, 0L, 0L, 0L)),
                new RoomMetricHistoryRepository.RoomMetricRecord("live-1",
                        new QueueStatisticsPoint(later, 100L, 2L, 8L, 0L, 0L, 0L, 0L, 0L)),
                new RoomMetricHistoryRepository.RoomMetricRecord("live-2",
                        new QueueStatisticsPoint(from, 100L, 3L, 4L, 0L, 0L, 0L, 0L, 0L)),
                new RoomMetricHistoryRepository.RoomMetricRecord("live-2",
                        new QueueStatisticsPoint(later, 100L, 8L, 1L, 0L, 0L, 0L, 0L, 0L))
        ));
        when(repository.findConcurrentGauges(List.of("live-1", "live-2"), from, to, "1m")).thenReturn(List.of(
                new RoomMetricHistoryRepository.ConcurrentGaugeRecord(from, 13L, 5L),
                new RoomMetricHistoryRepository.ConcurrentGaugeRecord(later, 10L, 9L)
        ));

        QueueStatistics result = service.getStatistics(from, to, null);

        assertThat(result.maxWaiting()).isEqualTo(13L);
        assertThat(result.maxActive()).isEqualTo(9L);
    }

    @Test
    void fallsBackToSeriesTimestampSumsWhenConcurrentQueryIsEmpty() {
        Instant later = from.plus(1, ChronoUnit.HOURS);
        List<RoomMetricHistoryRepository.RoomMetricRecord> records = List.of(
                new RoomMetricHistoryRepository.RoomMetricRecord("live-1",
                        new QueueStatisticsPoint(from, 100L, 10L, 1L, 0L, 0L, 0L, 0L, 0L)),
                new RoomMetricHistoryRepository.RoomMetricRecord("live-2",
                        new QueueStatisticsPoint(from, 100L, 3L, 4L, 0L, 0L, 0L, 0L, 0L)),
                new RoomMetricHistoryRepository.RoomMetricRecord("live-1",
                        new QueueStatisticsPoint(later, 100L, 2L, 8L, 0L, 0L, 0L, 0L, 0L)),
                new RoomMetricHistoryRepository.RoomMetricRecord("live-2",
                        new QueueStatisticsPoint(later, 100L, 8L, 1L, 0L, 0L, 0L, 0L, 0L))
        );

        assertThat(QueueStatisticsService.concurrentMaxWaiting(List.of(), records)).isEqualTo(13L);
        assertThat(QueueStatisticsService.concurrentMaxActive(List.of(), records)).isEqualTo(9L);
    }
}
