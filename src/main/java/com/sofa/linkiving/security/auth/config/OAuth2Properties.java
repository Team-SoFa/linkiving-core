package com.sofa.linkiving.security.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(
	String successRedirectUrl,
	String termsRedirectUrl,
	String failureRedirectUrl
) {
	public OAuth2Properties {
		if ((termsRedirectUrl == null || termsRedirectUrl.isBlank()) && successRedirectUrl != null) {
			termsRedirectUrl = successRedirectUrl.replaceFirst("/home/?$", "/terms");
		}
	}
}
