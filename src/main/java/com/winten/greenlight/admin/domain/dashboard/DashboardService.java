package com.winten.greenlight.admin.domain.dashboard;

import com.winten.greenlight.admin.db.repository.redis.room.RoomCacheRepository;
import com.winten.greenlight.admin.db.repository.redis.site.SiteCacheRepository;
import com.winten.greenlight.admin.support.error.NotModifiedException;
import com.winten.greenlight.admin.support.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final RoomCacheRepository roomCacheRepository;
    private final SiteCacheRepository siteCacheRepository;

    public DashboardDetail getDashboardDetail(String version) {
        String currentVersion = roomCacheRepository.getRoomMetricVersion();
        if (currentVersion.equals(version)) { // 버전 변경이 없을 경우 스킵
            throw new NotModifiedException();
        }

        var currentUser = AuthUtil.getCurrentUser();
        var roomIdList = siteCacheRepository.getEnabledRoomIdList(currentUser.getUserSiteId());

        var dashboardDetail = DashboardDetail.empty();

        for (String roomId : roomIdList) {
            var roomMetric = roomCacheRepository.getRoomMetric(roomId);
            dashboardDetail.getDetail().put(roomId, roomMetric);
        }
        dashboardDetail.setVersion(currentVersion);
        dashboardDetail.setInterval(3);
        return dashboardDetail;
    }
}