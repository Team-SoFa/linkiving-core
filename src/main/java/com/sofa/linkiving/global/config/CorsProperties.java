package com.sofa.linkiving.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
	List<String> allowedOrigins,
	List<String> extensionAllowedOrigins
) {
	public CorsProperties {
		allowedOrigins = normalizeOrigins(allowedOrigins);
		extensionAllowedOrigins = normalizeOrigins(extensionAllowedOrigins);
	}

	private static List<String> normalizeOrigins(List<String> origins) {
		if (origins == null || origins.isEmpty()) {
			return List.of();
		}
		return List.copyOf(origins);
	}
}
