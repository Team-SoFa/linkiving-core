package com.sofa.linkiving.domain.member.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.TokenRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.infra.redis.RedisKeyRegistry;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
	private final MemberCommandService memberCommandService;
	private final MemberQueryService memberQueryService;
	private final JwtTokenProvider jwtTokenProvider;
	private final RedisService redisService;

	public TokenRes signup(SignupReq req) {

		if (memberQueryService.existsMemberByEmail(req.email())) {
			throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
		}

		// TODO: Change this when Security dependency is added later
		String encoded = Base64.getEncoder()
			.encodeToString(req.password().getBytes(StandardCharsets.UTF_8));

		Member member = memberCommandService.addUser(req.email(), encoded);

		String accessToken = jwtTokenProvider.createAccessToken(member.getEmail());
		String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail());

		return TokenRes.of(accessToken, refreshToken);
	}

	@Transactional(readOnly = true)
	public TokenRes login(LoginReq req) {
		Member member = memberQueryService.getUser(req.email());

		// TODO: 추후 PasswordEncoder(BCrypt)로 변경 권장
		String encoded = Base64.getEncoder()
			.encodeToString(req.password().getBytes(StandardCharsets.UTF_8));

		if (!member.verifyPassword(encoded)) {
			throw new BusinessException(MemberErrorCode.INCORRECT_PASSWORD);
		}

		String accessToken = jwtTokenProvider.createAccessToken(member.getEmail());
		String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail());

		return TokenRes.of(accessToken, refreshToken);
	}

	public void logout(Member member) {
		redisService.delete(RedisKeyRegistry.REFRESH_TOKEN, member.getEmail());
	}
}
