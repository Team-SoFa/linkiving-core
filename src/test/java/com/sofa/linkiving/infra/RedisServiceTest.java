package com.sofa.linkiving.infra;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.sofa.linkiving.infra.redis.RedisKeyRegistry;
import com.sofa.linkiving.infra.redis.RedisKeySpec;
import com.sofa.linkiving.infra.redis.RedisService;

@ExtendWith(MockitoExtension.class)
public class RedisServiceTest {

	@Mock
	StringRedisTemplate redisTemplate;

	@Mock
	ValueOperations<String, String> valueOps;

	@InjectMocks
	RedisService redisService;

	/* -------- Raw key APIs -------- */

	@Test
	@DisplayName("키/값을 저장")
	void shouldSaveValue() {
		// given
		String key = "key";
		String value = "value";

		given(redisTemplate.opsForValue()).willReturn(valueOps);

		// when
		redisService.save(key, value);

		// then & verify
		verify(valueOps, times(1)).set(key, value);
	}

	@Test
	@DisplayName("TTL과 함께 키/값을 저장")
	void shouldSaveValueWithTtl() {
		// given
		String key = "key";
		String value = "value";
		Duration ttl = Duration.ofMinutes(5);

		given(redisTemplate.opsForValue()).willReturn(valueOps);

		// when
		redisService.save(key, value, ttl);

		// then
		verify(valueOps, times(1)).set(key, value, ttl);
	}

	@Test
	@DisplayName("키로 값을 조회한다")
	void shouldGetValue() {
		// given
		String key = "key";
		String value = "value";
		given(valueOps.get(key)).willReturn(value);

		given(redisTemplate.opsForValue()).willReturn(valueOps);

		// when
		String result = redisService.get(key);

		// then
		assertThat(result).isEqualTo(value);
		verify(valueOps, times(1)).get(key);
	}

	@Test
	@DisplayName("키가 없으면 hasNo가 true를 반환한다")
	void shouldReturnTrueWhenKeyNotExists() {
		// given
		String key = "key";

		given(redisTemplate.hasKey(key)).willReturn(false);

		// when
		boolean result = redisService.hasNoKey(key);

		// then
		assertThat(result).isTrue();
		verify(redisTemplate).hasKey(key);
	}

	@Test
	@DisplayName("키가 있으면 hasNo가 false를 반환한다")
	void shouldReturnFalseWhenKeyExists() {
		// given
		String key = "key";

		given(redisTemplate.hasKey(key)).willReturn(true);

		// when
		boolean result = redisService.hasNoKey(key);

		// then
		assertThat(result).isFalse();
		verify(redisTemplate).hasKey(key);
	}

	@Test
	@DisplayName("키를 삭제한다")
	void shouldDeleteKey() {
		// when
		String key = "key";

		redisService.delete(key);

		// then
		verify(redisTemplate, times(1)).delete(key);
	}

	/* -------- Spec-based APIs -------- */

	@Test
	@DisplayName("스펙 기반 키 생성과 TTL 프로퍼티가 적용된다")
	void shouldSaveUsingSpecAndTtlProperties() {
		// given
		RedisKeySpec spec = RedisKeyRegistry.REFRESH_TOKEN; //prefix: rt, duration: 180d
		String token = "exampleToken";

		given(redisTemplate.opsForValue()).willReturn(valueOps);

		// when
		redisService.save(spec, token, "user", "42");

		// then
		verify(valueOps, times(1)).set("rt:user:42", token, spec.defaultTtl());
	}

	@Test
	@DisplayName("스펙과 파츠를 조합해 키로 값을 조회한다")
	void shouldGetValueBySpecAndParts() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOps);

		RedisKeySpec spec = RedisKeyRegistry.REFRESH_TOKEN;
		String expectedKey = "rt:user:42";
		String token = "exampleToken";
		given(valueOps.get(expectedKey)).willReturn(token);

		// when
		String value = redisService.get(spec, "user", "42");

		// then
		assertThat(value).isEqualTo(token);
		verify(valueOps, times(1)).get(expectedKey);
		verifyNoMoreInteractions(valueOps);
	}

	@Test
	@DisplayName("스펙과 파츠를 조합해 hasKey=true/false를 판별한다")
	void shouldCheckHasKeyBySpecAndParts() {
		// given
		RedisKeySpec spec = RedisKeyRegistry.REFRESH_TOKEN;
		String expectedKey = "rt:user:42";

		// when (exists = true)
		given(redisTemplate.hasKey(expectedKey)).willReturn(true);
		boolean exists = redisService.hasNoKey(spec, "user", "42");

		// then
		assertThat(exists).isFalse();
		verify(redisTemplate, times(1)).hasKey(expectedKey);

		// when (exists = false)
		reset(redisTemplate);
		given(redisTemplate.hasKey(expectedKey)).willReturn(false);
		boolean notExists = redisService.hasNoKey(spec, "user", "42");

		// then
		assertThat(notExists).isTrue();
		verify(redisTemplate, times(1)).hasKey(expectedKey);
	}

	@Test
	@DisplayName("스펙과 파츠를 조합해 키를 삭제한다")
	void shouldDeleteBySpecAndParts() {
		// given
		RedisKeySpec spec = RedisKeyRegistry.REFRESH_TOKEN;
		String expectedKey = "rt:user:42";

		// when
		redisService.delete(spec, "user", "42");

		// then
		verify(redisTemplate, times(1)).delete(expectedKey);
		verifyNoMoreInteractions(redisTemplate);
	}
}
