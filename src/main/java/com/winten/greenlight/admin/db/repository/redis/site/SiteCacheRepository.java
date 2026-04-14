package com.winten.greenlight.admin.db.repository.redis.site;

import com.winten.greenlight.admin.domain.site.SiteInfo;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SiteCacheRepository {
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> jsonRedisTemplate;
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

//    public void updateRoomListCache(List<Room> rooms) {
//        var currentUser = AuthUtil.getCurrentUser();
//        String key = redisKeyBuilder.siteRoomIdList(currentUser.getUserSiteId());
//
//        // Enable 상태인 room만 입력
//        var roomIdList = rooms.stream()
//                .filter(Room::getEnabled)
//                .map(Room::getRoomId)
//                .toList();
//        String value = jsonMapper.writeValueAsString(roomIdList);
//
//        redisTemplate.opsForValue().set(key, value);
//    }

    public void updateSiteInfo(SiteInfo siteInfo) {
        String key = redisKeyBuilder.siteInfoMeta(siteInfo.getSiteId());
        var siteInfoMap = new HashMap<String, Object>();
        siteInfoMap.put("siteEnabled", siteInfo.isSiteEnabled());
        jsonRedisTemplate.opsForHash().putAll(key, siteInfoMap);
    }

//    public List<String> getSiteRoomIdList(String siteId) {
//        String key = redisKeyBuilder.siteRoomIdList(siteId);
//        String value = redisTemplate.opsForValue().get(key);
//
//        if (value == null || value.isBlank()) {
//            return Collections.emptyList();
//        }
//
//        try {
//            return jsonMapper.readValue(value, new TypeReference<>() {});
//        } catch (Exception e) {
//            log.error("Failed to parse Room Id List", e);
//            throw CoreException.of(ErrorType.REDIS_ERROR, "Redis 조회 중 readValue 실패. " + e.getMessage());
//        }
//    }
}