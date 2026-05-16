package com.sofa.linkiving.domain.auth.dto.internal;

public record TokenDto(
	String accessToken,
	int accessExp,
	String refreshToken,
	int refreshExp
) {
}
