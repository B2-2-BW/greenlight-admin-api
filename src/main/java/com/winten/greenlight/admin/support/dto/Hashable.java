package com.winten.greenlight.admin.support.dto;

import com.winten.greenlight.admin.db.repository.redis.RedisWriter;

/**
 * Redis에 저장할 때 DTO -> Map 전환이 가능한 클래스임을 명시해주는 마커 인터페이스
 * @see RedisWriter
 */
public interface Hashable {
}