package com.sofa.linkiving.global.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "analytics.ga4")
public record Ga4Properties(
	boolean enabled,
	String measurementId,
	String apiSecret,
	String endpoint
) {
	private static final String DEFAULT_ENDPOINT = "https://www.google-analytics.com";

	public Ga4Properties {
		if (!StringUtils.hasText(endpoint)) {
			endpoint = DEFAULT_ENDPOINT;
		}
	}

	public boolean isReady() {
		return enabled && StringUtils.hasText(measurementId) && StringUtils.hasText(apiSecret);
	}

	@Override
	public String toString() {
		return "Ga4Properties[enabled=%s, measurementId=%s, apiSecret=****, endpoint=%s]"
			.formatted(enabled, measurementId, endpoint);
	}
}
