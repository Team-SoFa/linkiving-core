package com.sofa.linkiving.domain.link.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "summary.worker")
public record SummaryWorkerProperties(
	long sleepMs
) {
	public SummaryWorkerProperties {
		if (sleepMs <= 0) {
			throw new IllegalArgumentException("sleepMs must be positive");
		}
	}
}