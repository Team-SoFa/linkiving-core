package com.sofa.linkiving.infra.redis;

import java.time.Duration;

public record RedisKeySpec(String prefix, Duration defaultTtl) {
	public String key(String... parts) {
		return parts == null || parts.length == 0
			? prefix
			: prefix + ":" + String.join(":", parts);
	}
}
