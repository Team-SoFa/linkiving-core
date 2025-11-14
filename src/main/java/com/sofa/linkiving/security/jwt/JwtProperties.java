package com.sofa.linkiving.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
	@NotBlank String secret,
	@Min(1) long accessTokenValidMinute,
	@Min(1) long refreshTokenValidMonth
) {
	public long accessTokenValidTime() {
		return accessTokenValidMinute * 60 * 1000L;
	}

	public long refreshTokenValidTime() {
		return refreshTokenValidMonth * 30L * 24 * 60 * 60 * 1000L;
	}
}
