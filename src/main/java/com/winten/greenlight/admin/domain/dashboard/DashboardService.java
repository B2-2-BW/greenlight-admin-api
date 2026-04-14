package com.winten.greenlight.admin.domain.dashboard;

import com.winten.greenlight.admin.db.repository.redis.room.RoomCacheRepository;
import com.winten.greenlight.admin.support.error.NotModifiedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final RoomCacheRepository roomCacheRepository;

    public DashboardDetail getDashboardDetail(String version, List<String> roomIdList) {
        String currentVersion = roomCacheRepository.getRoomMetricVersion();
        if (currentVersion.equals(version)) { // 버전 변경이 없을 경우 스킵
            throw new NotModifiedException();
        }

        var dashboardDetail = DashboardDetail.empty();

        if (roomIdList != null) {
            for (String roomId : roomIdList) {
                var roomMetric = roomCacheRepository.getRoomMetric(roomId);
                dashboardDetail.getDetail().put(roomId, roomMetric);
            }
        }
        dashboardDetail.setVersion(currentVersion);
        dashboardDetail.setInterval(3);

        return dashboardDetail;
    }
}