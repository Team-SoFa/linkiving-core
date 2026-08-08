package com.sofa.linkiving.global.analytics;

public record AnalyticsContext(
	String clientId,
	String source
) {
	public static final String SOURCE_WEB = "web";
	public static final String SOURCE_EXTENSION = "extension";
	public static final String DEFAULT_SOURCE = SOURCE_WEB;

	public static AnalyticsContext of(String clientId, String source) {
		return new AnalyticsContext(clientId, source);
	}
}
