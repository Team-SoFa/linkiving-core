package com.sofa.linkiving.domain.member.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.member.withdrawal")
public record MemberWithdrawalProperties(
	boolean enabled,
	String internalSecret,
	Duration recentAuthenticationMaxAge
) {
	public MemberWithdrawalProperties {
		if (recentAuthenticationMaxAge == null) {
			recentAuthenticationMaxAge = Duration.ofMinutes(10);
		}
	}
}
