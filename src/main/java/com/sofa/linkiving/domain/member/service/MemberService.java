package com.sofa.linkiving.domain.member.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.dto.request.MemberWithdrawalReq;
import com.sofa.linkiving.domain.member.dto.request.TermsAgreementReq;
import com.sofa.linkiving.domain.member.dto.response.MemberProfileRes;
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
	private final MemberQueryService memberQueryService;
	private final JwtTokenProvider jwtTokenProvider;
	private final RedisService redisService;
	private final MemberWithdrawalService memberWithdrawalService;
	@Value("${app.member.current-terms-version:2026-08-03}")
	private String currentTermsVersion;
	@Value("${app.member.current-privacy-version:2026-08-03}")
	private String currentPrivacyVersion;

	public void logout(Member member) {
		redisService.delete(RedisKeyRegistry.REFRESH_TOKEN, member.getEmail());
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void withdraw(Member member, MemberWithdrawalReq req) {
		memberWithdrawalService.withdraw(member.getId(), member.getEmail(), req.clientId(), req.deleteReason());
	}

	@Transactional(readOnly = true)
	public MemberProfileRes getProfile(Member member) {
		return MemberProfileRes.from(member);
	}

	public TokenRes agreeTerms(Member member, TermsAgreementReq req) {
		validateAgreementVersion(req);

		Member managed = memberQueryService.getUser(member.getEmail());
		managed.agreeTerms(req.termsVersion(), req.privacyVersion());

		String accessToken = jwtTokenProvider.createAccessToken(managed);
		String refreshToken = jwtTokenProvider.createRefreshToken(managed.getEmail());

		return TokenRes.of(accessToken, refreshToken);
	}

	private void validateAgreementVersion(TermsAgreementReq req) {
		if (!currentTermsVersion.equals(req.termsVersion()) || !currentPrivacyVersion.equals(req.privacyVersion())) {
			throw new BusinessException(MemberErrorCode.INVALID_TERMS_VERSION);
		}
	}
}
