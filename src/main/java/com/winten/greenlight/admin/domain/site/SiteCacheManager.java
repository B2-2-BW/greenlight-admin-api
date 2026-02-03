package com.winten.greenlight.admin.domain.site;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class SiteCacheManager {
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> redisTemplate;

    public void updateSiteApiKeyCache(final SiteInfo siteInfo) {
        var key = redisKeyBuilder.siteApiKey(siteInfo.getSiteApiKey());

        try {
            redisTemplate.opsForValue().set(key, siteInfo.getSiteId());
        } catch (RedisException e) {
            log.error(e.getMessage());
            throw new CoreException(ErrorType.REDIS_ERROR, e);
        }
    }
}