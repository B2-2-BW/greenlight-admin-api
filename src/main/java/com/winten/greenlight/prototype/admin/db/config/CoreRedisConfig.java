package com.winten.greenlight.prototype.admin.db.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.prototype.admin.db.repository.redis.EventEntity;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class CoreRedisConfig {
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(RedisProperties properties) {
        var config = new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
        config.setPassword(properties.getPassword());
        return new LettuceConnectionFactory(config);
    }
    @Bean
    public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }
    @Bean
    public RedisTemplate<String, EventEntity> eventRedisTemplate(LettuceConnectionFactory factory, ObjectMapper objectMapper) {
        RedisTemplate<String, EventEntity> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);

        RedisSerializer<EventEntity> eventSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, EventEntity.class);

        redisTemplate.setValueSerializer(eventSerializer);
        redisTemplate.setHashValueSerializer(eventSerializer);

        return redisTemplate;
    }
}