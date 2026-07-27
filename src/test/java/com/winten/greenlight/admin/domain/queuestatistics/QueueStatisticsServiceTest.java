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

        QueueStatistics result = service.getStatistics(from, to, null);

        assertThat(result.availableRooms()).extracting(QueueStatisticsRoomOption::roomId).containsExactly("live-1", "live-2");
        assertThat(result.series()).extracting(QueueStatisticsRoom::roomId).containsExactly("live-1", "live-2");
        assertThat(result.series()).allSatisfy(series -> assertThat(series.points()).isEmpty());
    }

    @Test
    void validatesRangeAndUsesExpectedWindow() {
        RoomService roomService = mock(RoomService.class);
        RoomMetricHistoryRepository repository = mock(RoomMetricHistoryRepository.class);
        QueueStatisticsService service = new QueueStatisticsService(roomService, repository);
        assertThatThrownBy(() -> service.getStatistics(from, from, null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.getStatistics(from.minus(8, ChronoUnit.DAYS), from, null)).isInstanceOf(CoreException.class);
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
        Room room = Room.builder().roomId("live-1").name("One").roomEnvironment(RoomEnvironment.LIVE).build();
        QueueStatisticsPoint point = new QueueStatisticsPoint(from, 100L, 10L, 30L, 45L, 5L, 4L, 3L, 1L);
        when(roomService.getRoomListFiltered(any(Room.class))).thenReturn(List.of(room));
        when(repository.findByRoomIds(List.of("live-1"), from, to, "1m"))
                .thenReturn(List.of(new RoomMetricHistoryRepository.RoomMetricRecord("live-1", point)));

        QueueStatistics result = service.getStatistics(from, to, List.of("live-1"));

        assertThat(result.series()).singleElement().satisfies(series -> {
            assertThat(series.name()).isEqualTo("One");
            assertThat(series.points()).containsExactly(point);
        });
    }
}
