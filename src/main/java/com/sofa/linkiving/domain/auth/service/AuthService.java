package com.sofa.linkiving.domain.auth.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.auth.dto.internal.TokenDto;
import com.sofa.linkiving.security.jwt.JwtProperties;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;

	public TokenDto reissue(String refreshToken) {

		String email = jwtTokenProvider.validateRefreshToken(refreshToken);

		String newAccessToken = jwtTokenProvider.createAccessToken(email);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

		int accessExp = (int)(jwtProperties.accessTokenValidTime() / 1000);
		int refreshExp = (int)(jwtProperties.refreshTokenValidTime() / 1000);

		return new TokenDto(newAccessToken, accessExp, newRefreshToken, refreshExp);
	}
}
