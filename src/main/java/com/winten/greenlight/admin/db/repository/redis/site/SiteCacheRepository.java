package com.winten.greenlight.admin.db.repository.redis.site;

import com.winten.greenlight.admin.domain.room.Room;
import com.winten.greenlight.admin.domain.site.SiteInfo;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SiteCacheRepository {
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;

    public void updateSiteApiKeyCache(final SiteInfo siteInfo) {
        var key = redisKeyBuilder.siteApiKey(siteInfo.getSiteApiKey());

        try {
            redisTemplate.opsForValue().set(key, siteInfo.getSiteId());
        } catch (RedisException e) {
            log.error(e.getMessage());
            throw new CoreException(ErrorType.REDIS_ERROR, e);
        }
    }

    public void updateEnabledRoomList(List<Room> rooms) {
        var currentUser = AuthUtil.getCurrentUser();
        String key = redisKeyBuilder.siteRoomList(currentUser.getSiteId());

        // Enable 상태인 room만 입력
        var roomIdList = rooms.stream()
                .filter(Room::getEnabled)
                .map(Room::getRoomId)
                .toList();
        String value = jsonMapper.writeValueAsString(roomIdList);

        redisTemplate.opsForValue().set(key, value);
    }

    public List<String> getEnabledRoomIdList(String siteId) {
        String key = redisKeyBuilder.siteRoomList(siteId);
        String value = redisTemplate.opsForValue().get(key);

        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return jsonMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse Room Id List", e);
            throw CoreException.of(ErrorType.REDIS_ERROR, "Redis 조회 중 readValue 실패. " + e.getMessage());
        }
    }
}