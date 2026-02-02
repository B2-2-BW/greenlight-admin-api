package com.winten.greenlight.admin.domain.room;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomEntity;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCacheManager {
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpleBeanPropertyFilter roomFilter = SimpleBeanPropertyFilter.serializeAllExcept(
            "userRole",
            "description",
            "createdBy",
            "createdAt",
            "createdIp",
            "updatedBy",
            "updatedAt",
            "updatedIp"
    );
    private final SimpleBeanPropertyFilter roomRuleFilter = SimpleBeanPropertyFilter.serializeAllExcept(
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
            String value = objectMapper.writer(filters).writeValueAsString(room);
            redisTemplate.opsForValue().set(key, value);
        } catch (JsonProcessingException e) {
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