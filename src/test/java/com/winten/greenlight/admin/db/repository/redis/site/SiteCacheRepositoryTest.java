package com.winten.greenlight.admin.db.repository.redis.site;

import com.winten.greenlight.admin.domain.site.SiteInfo;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteCacheRepositoryTest {
    @Test
    @SuppressWarnings("unchecked")
    void writesSiteAndQueueEnabledToTheSameSiteMetaHash() {
        RedisKeyBuilder keyBuilder = mock(RedisKeyBuilder.class);
        RedisTemplate<String, String> stringRedisTemplate = mock(RedisTemplate.class);
        RedisTemplate<String, Object> jsonRedisTemplate = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(keyBuilder.siteInfoMeta("site-a")).thenReturn("greenlight:site:site-a:meta");
        when(jsonRedisTemplate.opsForHash()).thenReturn(hashOperations);
        var repository = new SiteCacheRepository(
                keyBuilder,
                stringRedisTemplate,
                jsonRedisTemplate,
                mock(JsonMapper.class)
        );

        repository.updateSiteInfo(SiteInfo.builder()
                .siteId("site-a")
                .siteEnabled(false)
                .queueEnabled(true)
                .build());

        verify(hashOperations).putAll(
                eq("greenlight:site:site-a:meta"),
                eq(Map.of("siteEnabled", false, "queueEnabled", true))
        );
    }
}
