package com.sofa.linkiving.infra.redis;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {

	private final StringRedisTemplate redisTemplate;
	private final RedisTtlProperties ttlProps;

	/* -------- Raw key APIs -------- */

	public void save(String key, String value) {
		ValueOperations<String, String> ops = redisTemplate.opsForValue();
		ops.set(key, value);
	}

	public void save(String key, String value, Duration ttl) {
		ValueOperations<String, String> ops = redisTemplate.opsForValue();
		ops.set(key, value, ttl);
	}

	public String get(String key) {
		ValueOperations<String, String> ops = redisTemplate.opsForValue();
		return ops.get(key);
	}

	public boolean hasNoKey(String key) {
		Boolean exists = redisTemplate.hasKey(key);
		return !exists;
	}

	public void delete(String key) {
		redisTemplate.delete(key);
	}

	/* -------- Spec-based APIs -------- */

	public void save(RedisKeySpec type, String value, String... keys) {
		String key = type.key(keys);
		Duration ttl = ttlProps.getOrDefault(type.name(), type.defaultTtl());
		ValueOperations<String, String> ops = redisTemplate.opsForValue();
		ops.set(key, value, ttl);
	}

	public String get(RedisKeySpec type, String... keys) {
		ValueOperations<String, String> ops = redisTemplate.opsForValue();
		return ops.get(type.key(keys));
	}

	public boolean hasNoKey(RedisKeySpec type, String... keys) {
		Boolean exists = redisTemplate.hasKey(type.key(keys));
		return !exists;
	}

	public void delete(RedisKeySpec type, String... keys) {
		redisTemplate.delete(type.key(keys));
	}
}
