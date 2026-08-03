package com.sofa.linkiving.global.analytics;

public record AnalyticsContext(
	String clientId,
	String source
) {
	public static AnalyticsContext of(String clientId, String source) {
		return new AnalyticsContext(clientId, source);
	}
}
