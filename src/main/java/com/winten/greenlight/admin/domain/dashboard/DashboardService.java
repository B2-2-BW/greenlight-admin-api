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
            return this.generateMockData(rooms, windowSeconds);
        }

        for (Room room : rooms) {
            if (!room.getEnabled()) continue;
            var roomStatus = dashboardCacheRepository.getRoomStatus(room.getRoomId(), windowSeconds);
            roomStatus.setRoomCapacity(room.getCapacity());
            // TODO 지금은 window size가 3일테지만, 이후에 변하게되면 예상시간 측정이 달라질수도 있음
            //  Core API와 예상시간 계산 로직을 통일해야함
            var estimatedWaitTime = roomStatus.getEnteredRate() != 0
                    ? roomStatus.getActiveCustomerCount() / roomStatus.getEnteredRate()
                    : 0 ;

            roomStatus.setEnteredRate(roomStatus.getEntered() / (double) windowSeconds);
            roomStatus.setInflowRate(roomStatus.getInflow() / (double) windowSeconds);
            roomStatus.setOutflowRate(roomStatus.getOutflow() / (double) windowSeconds);

            roomStatus.setEstimatedWaitTime(estimatedWaitTime);
            dashboardDetail.getDetail().put(room.getRoomId(), roomStatus);
        }
        return dashboardDetail;
    }

    // 가짜 데이터 생성 (대시보드 보여주기용)
    private DashboardDetail generateMockData(List<Room> rooms, long windowSeconds) {
        var dashboardDetail = DashboardDetail.empty();
        for (Room room : rooms) {
            if (!room.getEnabled()) continue;
            var roomCapacity = room.getCapacity();
            var waitingCount = ThreadLocalRandom.current().nextInt(300, 2001);
            var activeCustomerCount = ThreadLocalRandom.current().nextInt(room.getCapacity()/4, room.getCapacity() + 1);
            var inflow = ThreadLocalRandom.current().nextInt(1, 121);
            var entered = ThreadLocalRandom.current().nextInt(1, 101);
            var outflow = entered + ThreadLocalRandom.current().nextInt(-20, +21);
            var inflowRate = inflow / (double) windowSeconds;
            var enteredRate = entered / (double) windowSeconds;
            var outflowRate = outflow / (double) windowSeconds;
            var estimatedWaitTime = activeCustomerCount / enteredRate;

            var roomStatus = RoomStatus.builder()
                    .roomId(room.getRoomId())
                    .roomCapacity(roomCapacity)
                    .waitingCount(waitingCount)
                    .activeCustomerCount(activeCustomerCount)
                    .inflow(inflow)
                    .entered(entered)
                    .outflow(outflow)
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