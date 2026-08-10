package com.sofa.linkiving.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.auth.dto.internal.TokenDto;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.domain.member.service.MemberQueryService;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.security.jwt.JwtProperties;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final MemberQueryService memberQueryService;

	@Transactional
	public TokenDto reissue(String refreshToken) {

		String email = jwtTokenProvider.validateRefreshToken(refreshToken);
		Member member = memberQueryService.getUserForUpdate(email);
		if (member.isWithdrawing()) {
			throw new BusinessException(MemberErrorCode.WITHDRAWAL_IN_PROGRESS);
		}

		String newAccessToken = jwtTokenProvider.createAccessToken(member);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

		int accessExp = (int)(jwtProperties.accessTokenValidTime() / 1000);
		int refreshExp = (int)(jwtProperties.refreshTokenValidTime() / 1000);

		return new TokenDto(newAccessToken, accessExp, newRefreshToken, refreshExp);
	}
}
