package com.winten.greenlight.admin.db.repository.redis.room;

import com.winten.greenlight.admin.db.repository.mapper.room.RoomEntity;
import com.winten.greenlight.admin.domain.room.RoomMetric;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoomCacheRepository {

    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;


    public void updateRoomMetaCache(final RoomEntity room) {
        try {
            var filters = new SimpleFilterProvider()
                    .addFilter("roomFilter", JsonFilters.roomFilter)
                    .addFilter("roomRuleFilter", JsonFilters.roomRuleFilter);
            String key = redisKeyBuilder.roomMeta(room.getRoomId());
            String value = jsonMapper.writer(filters).writeValueAsString(room);
            redisTemplate.opsForValue().set(key, value);
        } catch (JacksonException e) {
            throw new CoreException(ErrorType.FAILED_TO_PARSE_JSON, e);
        } catch (RedisException e) {
            log.error(e.getMessage());
            throw new CoreException(ErrorType.REDIS_ERROR, e);
        }
    }

    public void deleteRoomMetaCache(final String roomId) {
        String key = redisKeyBuilder.roomMeta(roomId);
        redisTemplate.delete(key);
    }

    public String getRoomMetricVersion() {
        var key = redisKeyBuilder.roomMetricVersion();
        var version = redisTemplate.opsForValue().get(key);
        return (version != null) ? version : "0";
    }

    public RoomMetric getRoomMetric(String roomId) {
        var key = redisKeyBuilder.roomMetricLatest(roomId);
        var value = redisTemplate.opsForValue().get(key);
        return jsonMapper.readValue(value, RoomMetric.class);
    }
}