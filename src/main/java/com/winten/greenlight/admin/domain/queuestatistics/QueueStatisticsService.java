package com.winten.greenlight.admin.domain.queuestatistics;

import com.winten.greenlight.admin.db.repository.influxdb.room.RoomMetricHistoryRepository;
import com.winten.greenlight.admin.domain.room.Room;
import com.winten.greenlight.admin.domain.room.RoomEnvironment;
import com.winten.greenlight.admin.domain.room.RoomService;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 대기열 통계. LIVE 방만 조회하며, 최대 대기/체류는 방별 max의 합이 아니라 동시 최댓값을 쓴다.
 */
@Service
@RequiredArgsConstructor
public class QueueStatisticsService {
    static final Duration MAX_RANGE = Duration.ofDays(7);
    /** 내보내기 1분 창은 Influx 포인트 수가 많아서 최대 1일. */
    static final Duration MAX_RANGE_1M = Duration.ofDays(1);
    static final Duration MAX_RANGE_10M = Duration.ofDays(7);
    static final Duration MAX_RANGE_1H = Duration.ofDays(15);

    private final RoomService roomService;
    private final RoomMetricHistoryRepository repository;

    public QueueStatistics getStatistics(Instant from, Instant to, List<String> requestedRoomIds) {
        return getStatistics(from, to, requestedRoomIds, null);
    }

    public QueueStatistics getStatistics(Instant from, Instant to, List<String> requestedRoomIds, String requestedWindow) {
        String window = resolveWindow(from, to, requestedWindow);
        validateRange(from, to, window);

        // 스케줄러가 Influx에 쓰는 것도 LIVE 방뿐이다.
        List<Room> availableLiveRooms = roomService.getRoomListFiltered(Room.builder()
                .roomEnvironment(RoomEnvironment.LIVE)
                .build());
        Map<String, Room> availableById = availableLiveRooms.stream()
                .collect(Collectors.toMap(Room::getRoomId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));

        List<Room> selectedRooms = selectRooms(requestedRoomIds, availableById);
        List<String> selectedRoomIds = selectedRooms.stream().map(Room::getRoomId).toList();
        List<RoomMetricHistoryRepository.RoomMetricRecord> metricRecords = selectedRooms.isEmpty()
                ? List.of()
                : repository.findByRoomIds(selectedRoomIds, from, to, window);
        List<RoomMetricHistoryRepository.ConcurrentGaugeRecord> concurrentGauges = selectedRooms.isEmpty()
                ? List.of()
                : repository.findConcurrentGauges(selectedRoomIds, from, to, window);
        Map<String, List<QueueStatisticsPoint>> pointsByRoomId = metricRecords
                .stream()
                .collect(Collectors.groupingBy(RoomMetricHistoryRepository.RoomMetricRecord::roomId,
                        LinkedHashMap::new,
                        Collectors.mapping(RoomMetricHistoryRepository.RoomMetricRecord::point, Collectors.toList())));

        List<QueueStatisticsRoomOption> availableRooms = availableLiveRooms.stream().map(this::toAvailableRoom).toList();
        List<QueueStatisticsRoom> series = selectedRooms.stream()
                .map(room -> new QueueStatisticsRoom(
                        room.getRoomId(),
                        room.getName(),
                        room.getEnabled(),
                        pointsByRoomId.getOrDefault(room.getRoomId(), List.of())))
                .toList();
        return new QueueStatistics(
                from, to, windowSeconds(window), availableRooms, series,
                concurrentMaxWaiting(concurrentGauges, metricRecords),
                concurrentMaxActive(concurrentGauges, metricRecords));
    }

    static long concurrentMaxWaiting(
            List<RoomMetricHistoryRepository.ConcurrentGaugeRecord> concurrentGauges,
            List<RoomMetricHistoryRepository.RoomMetricRecord> metricRecords
    ) {
        return concurrentMax(
                concurrentGauges, RoomMetricHistoryRepository.ConcurrentGaugeRecord::totalWaiting,
                metricRecords, QueueStatisticsPoint::totalWaiting);
    }

    static long concurrentMaxActive(
            List<RoomMetricHistoryRepository.ConcurrentGaugeRecord> concurrentGauges,
            List<RoomMetricHistoryRepository.RoomMetricRecord> metricRecords
    ) {
        return concurrentMax(
                concurrentGauges, RoomMetricHistoryRepository.ConcurrentGaugeRecord::totalActive,
                metricRecords, QueueStatisticsPoint::totalActive);
    }

    /**
     * 동시 최댓값. Influx last→방 합 시계열이 있으면 그 최댓값,
     * 없으면 방별 last 시계열을 같은 시각끼리 더한 뒤 최댓값.
     */
    private static long concurrentMax(
            List<RoomMetricHistoryRepository.ConcurrentGaugeRecord> concurrentGauges,
            Function<RoomMetricHistoryRepository.ConcurrentGaugeRecord, Long> concurrentGetter,
            List<RoomMetricHistoryRepository.RoomMetricRecord> metricRecords,
            Function<QueueStatisticsPoint, Long> pointGetter
    ) {
        if (concurrentGauges != null && !concurrentGauges.isEmpty()) {
            return maxOrZero(concurrentGauges, concurrentGetter);
        }
        return maxByTimestampSum(metricRecords, pointGetter);
    }

    private static long maxOrZero(
            List<RoomMetricHistoryRepository.ConcurrentGaugeRecord> records,
            Function<RoomMetricHistoryRepository.ConcurrentGaugeRecord, Long> getter
    ) {
        long max = 0L;
        for (RoomMetricHistoryRepository.ConcurrentGaugeRecord record : records) {
            Long value = getter.apply(record);
            if (value != null && value > max) {
                max = value;
            }
        }
        return max;
    }

    static long maxByTimestampSum(
            List<RoomMetricHistoryRepository.RoomMetricRecord> records,
            Function<QueueStatisticsPoint, Long> getter
    ) {
        if (records == null || records.isEmpty()) {
            return 0L;
        }
        Map<Instant, Long> byTimestamp = new HashMap<>();
        for (RoomMetricHistoryRepository.RoomMetricRecord record : records) {
            QueueStatisticsPoint point = record.point();
            if (point == null || point.timestamp() == null) {
                continue;
            }
            Long value = getter.apply(point);
            byTimestamp.merge(point.timestamp(), value == null ? 0L : value, Long::sum);
        }
        return byTimestamp.values().stream().mapToLong(Long::longValue).max().orElse(0L);
    }

    private List<Room> selectRooms(List<String> requestedRoomIds, Map<String, Room> availableById) {
        if (requestedRoomIds == null || requestedRoomIds.isEmpty()) {
            return List.copyOf(availableById.values());
        }
        return requestedRoomIds.stream().distinct().map(roomId -> {
            Room room = availableById.get(roomId);
            if (room == null) {
                throw CoreException.of(ErrorType.ROOM_NOT_FOUND, "대기열을 찾을 수 없습니다. ID: " + roomId);
            }
            return room;
        }).toList();
    }

    static void validateRange(Instant from, Instant to) {
        validateRange(from, to, null);
    }

    static void validateRange(Instant from, Instant to, String window) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "from must be before to");
        }
        Duration maxRange = maxRangeForWindow(window);
        if (Duration.between(from, to).compareTo(maxRange) > 0) {
            throw CoreException.of(ErrorType.INVALID_DATA, "조회 기간이 선택한 시간 단위의 최대 범위를 초과했습니다.");
        }
    }

    static Duration maxRangeForWindow(String window) {
        if (window == null) {
            return MAX_RANGE;
        }
        return switch (window) {
            case "1m" -> MAX_RANGE_1M;
            case "10m" -> MAX_RANGE_10M;
            case "1h" -> MAX_RANGE_1H;
            default -> MAX_RANGE;
        };
    }

    /** 화면 조회는 기간으로 창을 고르고, 엑셀 보내기는 요청 창(1m/10m/1h)을 그대로 쓴다. */
    static String resolveWindow(Instant from, Instant to, String requestedWindow) {
        if (requestedWindow == null || requestedWindow.isBlank()) {
            return selectWindow(Duration.between(from, to));
        }
        String window = requestedWindow.trim();
        if (!window.equals("1m") && !window.equals("10m") && !window.equals("1h")) {
            throw CoreException.of(ErrorType.INVALID_DATA, "지원하지 않는 시간 단위입니다.");
        }
        return window;
    }

    static String selectWindow(Duration range) {
        if (range.compareTo(Duration.ofHours(6)) <= 0) return "1m";
        if (range.compareTo(Duration.ofHours(24)) <= 0) return "5m";
        return "30m";
    }

    private long windowSeconds(String window) {
        return switch (window) {
            case "1m" -> 60;
            case "5m" -> 300;
            case "10m" -> 600;
            case "1h" -> 3600;
            default -> 1800;
        };
    }

    private QueueStatisticsRoomOption toAvailableRoom(Room room) {
        return new QueueStatisticsRoomOption(room.getRoomId(), room.getName(), room.getDescription(), room.getEnabled());
    }
}
