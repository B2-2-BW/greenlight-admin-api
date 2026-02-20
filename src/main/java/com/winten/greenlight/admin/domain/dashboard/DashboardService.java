package com.winten.greenlight.admin.domain.dashboard;

import com.winten.greenlight.admin.api.controller.dashboard.DashboardRequest;
import com.winten.greenlight.admin.db.repository.redis.dashboard.DashboardCacheRepository;
import com.winten.greenlight.admin.domain.room.Room;
import com.winten.greenlight.admin.domain.room.RoomService;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final RoomService roomService;
    private final DashboardCacheRepository dashboardCacheRepository;

    public DashboardDetail getDashboardDetail(DashboardRequest request) {
        var window = Objects.requireNonNullElse(request.getWindow(), Duration.ofSeconds(1));

        long windowSeconds = window.toSeconds();
        if (windowSeconds < 1 || windowSeconds > 10) {
            throw CoreException.of(ErrorType.INVALID_DATA, "window는 1s에서 10s 사이여야 합니다.");
        }

        var dashboardDetail = DashboardDetail.empty();
        var rooms = roomService.getAllRoom();
        if (rooms.isEmpty()) {
            return dashboardDetail;
        }

        if (request.isMock()) {
            return this.generateMockData(rooms);
        }

        for  (Room room : rooms) {
            if (!room.getEnabled()) continue;
            var roomStatus = new RoomStatus();
            roomStatus.setRoomId(room.getRoomId());

            // TODO redis 데이터 추출한 뒤 set
            // TODO inflow는 어떻게 계산? WAITING LOG가 따로 필요해보임
            // TODO outflow는 어떻게 계산?
            // waitingCount // room:{roomId}:queue:WAITING size()
            // roomCapacity // room.getCapacity()
            // roomCustomerCount // room:{roomId}:heartbeat:ENTERED size
            // inflowRate // room:{roomId}:metrics:traffic:inflow (window size 만큼 추출 후 평균)
            // enteredRate // room:{roomId}:heartbeat:ENTERED ranged size (window size 만큼 추출 후 평균)
            // outflowRate // room:{roomId}:metrics:traffic:outflow (window size 만큼 추출 후 평균)
            // estimatedWaitTime // roomCustomerCount / 10초 평균 enteredRate;

        }
        return dashboardDetail;
    }

    private DashboardDetail generateMockData(List<Room> rooms) {
        var dashboardDetail = DashboardDetail.empty();
        for (Room room : rooms) {
            if (!room.getEnabled()) continue;
            var roomCapacity = room.getCapacity();
            var waitingCount = ThreadLocalRandom.current().nextInt(300, 2001);
            var roomCustomerCount = ThreadLocalRandom.current().nextInt(room.getCapacity()/3, room.getCapacity() + 1);
            var inflowRate = ThreadLocalRandom.current().nextInt(1, 121);
            var enteredRate = ThreadLocalRandom.current().nextInt(1, 101);
            var outflowRate = enteredRate + 3;
            var estimatedWaitTime = roomCustomerCount / enteredRate;

            var roomStatus = RoomStatus.builder()
                    .roomId(room.getRoomId())
                    .roomCapacity(roomCapacity)
                    .waitingCount(waitingCount)
                    .roomCustomerCount(roomCustomerCount)
                    .inflowRate(inflowRate)
                    .enteredRate(enteredRate)
                    .outflowRate(outflowRate)
                    .estimatedWaitTime(estimatedWaitTime)
                    .build();
            dashboardDetail.getDetail().put(room.getRoomId(), roomStatus);
        }
        return dashboardDetail;
    }
}