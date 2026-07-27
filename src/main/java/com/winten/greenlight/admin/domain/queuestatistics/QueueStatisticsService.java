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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueStatisticsService {
    static final Duration MAX_RANGE = Duration.ofDays(7);

    private final RoomService roomService;
    private final RoomMetricHistoryRepository repository;

    public QueueStatistics getStatistics(Instant from, Instant to, List<String> requestedRoomIds) {
        validateRange(from, to);

        List<Room> availableLiveRooms = roomService.getRoomListFiltered(Room.builder()
                .roomEnvironment(RoomEnvironment.LIVE)
                .build());
        Map<String, Room> availableById = availableLiveRooms.stream()
                .collect(Collectors.toMap(Room::getRoomId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));

        List<Room> selectedRooms = selectRooms(requestedRoomIds, availableById);
        String window = selectWindow(Duration.between(from, to));
        List<RoomMetricHistoryRepository.RoomMetricRecord> metricRecords = selectedRooms.isEmpty()
                ? List.of()
                : repository.findByRoomIds(selectedRooms.stream().map(Room::getRoomId).toList(), from, to, window);
        Map<String, List<QueueStatisticsPoint>> pointsByRoomId = metricRecords
                .stream()
                .collect(Collectors.groupingBy(RoomMetricHistoryRepository.RoomMetricRecord::roomId,
                        LinkedHashMap::new,
                        Collectors.mapping(RoomMetricHistoryRepository.RoomMetricRecord::point, Collectors.toList())));

        List<QueueStatisticsRoomOption> availableRooms = availableLiveRooms.stream().map(this::toAvailableRoom).toList();
        List<QueueStatisticsRoom> series = selectedRooms.stream()
                .map(room -> new QueueStatisticsRoom(room.getRoomId(), room.getName(), pointsByRoomId.getOrDefault(room.getRoomId(), List.of())))
                .toList();
        return new QueueStatistics(from, to, windowSeconds(window), availableRooms, series);
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
        if (from == null || to == null || !from.isBefore(to)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "from must be before to");
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw CoreException.of(ErrorType.INVALID_DATA, "The maximum queue statistics range is 7 days.");
        }
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
            default -> 1800;
        };
    }

    private QueueStatisticsRoomOption toAvailableRoom(Room room) {
        return new QueueStatisticsRoomOption(room.getRoomId(), room.getName());
    }
}
