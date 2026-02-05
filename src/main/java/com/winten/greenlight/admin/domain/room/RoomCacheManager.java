package com.winten.greenlight.admin.domain.room;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.PropertyFilter;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomEntity;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCacheManager {
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;
    private final PropertyFilter roomFilter = SimpleBeanPropertyFilter.serializeAllExcept(
            "userRole",
            "description",
            "createdBy",
            "createdAt",
            "createdIp",
            "updatedBy",
            "updatedAt",
            "updatedIp"
    );
    private final PropertyFilter roomRuleFilter = SimpleBeanPropertyFilter.serializeAllExcept(
            "siteId",
            "roomId",
            "ruleSeq",
            "userRole",
            "description",
            "createdBy",
            "createdAt",
            "createdIp",
            "updatedBy",
            "updatedAt",
            "updatedIp"
    );

    public void updateRoomMetaCache(final RoomEntity room) {
        try {
            var filters = new SimpleFilterProvider()
                    .addFilter("roomFilter", roomFilter)
                    .addFilter("roomRuleFilter", roomRuleFilter);
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
}