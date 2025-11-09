package com.sofa.linkiving.infra.redis;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis.ttl")
public record RedisTtlProperties(Map<String, Duration> values) {
	public Duration getOrDefault(String specName, Duration def) {
		if (values == null) {
			return def;
		}
		return values.getOrDefault(specName, def);
	}
}
