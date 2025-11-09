package com.sofa.linkiving.infra.redis;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RedisKeyRegistry {

	public static final RedisKeySpec REFRESH_TOKEN = new RedisKeySpec("rt", Duration.ofDays(180));
}
