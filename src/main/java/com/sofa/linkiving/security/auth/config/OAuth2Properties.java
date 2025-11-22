package com.sofa.linkiving.security.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(
	String successRedirectUrl,
	String failureRedirectUrl
) {
}
