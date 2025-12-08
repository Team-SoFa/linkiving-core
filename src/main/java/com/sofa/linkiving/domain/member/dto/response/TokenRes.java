package com.sofa.linkiving.domain.member.dto.response;

import lombok.Builder;

@Builder
public record TokenRes(
	String accessToken,
	String refreshToken
) {
	public static TokenRes of(String accessToken, String refreshToken) {
		return TokenRes.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}
}
