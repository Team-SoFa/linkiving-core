package com.sofa.linkiving.domain.link.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "summary.worker")
public record SummaryWorkerProperties(
	Duration sleepDuration
) {
	public SummaryWorkerProperties {
		if (sleepDuration == null || sleepDuration.isZero() || sleepDuration.isNegative()) {
			throw new IllegalArgumentException("sleepDuration must be positive");
		}
	}
}
