package com.winten.greenlight.admin.domain.dashboard;

import com.winten.greenlight.admin.domain.room.Room;
import com.winten.greenlight.admin.domain.room.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final RoomService roomService;
    private final RedisTemplate<String, String> stringRedisTemplate;

    public DashboardDetail getDashboardDetail() {
        DashboardDetail dashboardDetail = new DashboardDetail();
        var rooms = roomService.getAllRoom();
        if  (rooms.isEmpty()) {
            return dashboardDetail;
        }
        for  (Room room : rooms) {
            var roomStatus = new RoomStatus();
            roomStatus.setRoomId(room.getRoomId());

            // TODO redis 데이터 추출한 뒤 set
        }
        return dashboardDetail;
    }
}
