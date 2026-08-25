package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.member.dto.request.MemberWithdrawalReq;
import com.sofa.linkiving.domain.member.dto.request.TermsAgreementReq;
import com.sofa.linkiving.domain.member.dto.response.MemberProfileRes;
import com.sofa.linkiving.domain.member.dto.response.TokenRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberDeleteReason;
import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class MemberServiceTest {
	@Mock
	JwtTokenProvider jwtTokenProvider;
	@Mock
	RedisService redisService;
	@Mock
	MemberWithdrawalService memberWithdrawalService;
	@InjectMocks
	MemberService memberService;
	@Mock
	MemberQueryService memberQueryService;

	@Test
	@DisplayName("로그아웃 시 Redis에 저장된 refresh token 삭제")
	void shouldDeleteRefreshTokenOnLogout() {
		// given
		Member member = Member.builder().email("test@test.com").build();

		// when
		memberService.logout(member);

		// then
		verify(redisService, times(1)).delete(any(), eq(member.getEmail()));
	}

	@Test
	@DisplayName("회원 탈퇴를 회원 식별자와 이메일로 위임")
	void shouldDelegateWithdrawal() {
		Member member = mock(Member.class);
		given(member.getId()).willReturn(42L);
		given(member.getEmail()).willReturn("withdraw@test.com");

		MemberWithdrawalReq req = new MemberWithdrawalReq(true, MemberDeleteReason.OTHER, "123.456");
		memberService.withdraw(member, req);

		verify(memberWithdrawalService).withdraw(42L, "withdraw@test.com", "123.456", MemberDeleteReason.OTHER);
	}

	@Test
	void shouldAgreeTermsAndReturnTokens() {
		// given
		Member member = Member.builder()
			.email("oauth@test.com")
			.status(MemberStatus.PENDING_TERMS)
			.build();
		Member managed = Member.builder()
			.email("oauth@test.com")
			.status(MemberStatus.PENDING_TERMS)
			.build();
		TermsAgreementReq req = new TermsAgreementReq(true, true, "2026-08-03", "2026-08-03");

		ReflectionTestUtils.setField(memberService, "currentTermsVersion", "2026-08-03");
		ReflectionTestUtils.setField(memberService, "currentPrivacyVersion", "2026-08-03");
		given(memberQueryService.getUser(member.getEmail())).willReturn(managed);
		given(jwtTokenProvider.createAccessToken(managed)).willReturn("mock-access-token");
		given(jwtTokenProvider.createRefreshToken(managed.getEmail())).willReturn("mock-refresh-token");

		// when
		TokenRes res = memberService.agreeTerms(member, req);

		// then
		assertThat(res.accessToken()).isEqualTo("mock-access-token");
		assertThat(res.refreshToken()).isEqualTo("mock-refresh-token");
		assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING_TERMS);
		assertThat(managed.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		assertThat(managed.needsTermsAgreement()).isFalse();
		assertThat(managed.getTermsVersion()).isEqualTo("2026-08-03");
		assertThat(managed.getPrivacyVersion()).isEqualTo("2026-08-03");
		assertThat(managed.getTermsAgreedAt()).isNotNull();
		assertThat(managed.getPrivacyAgreedAt()).isNotNull();

		verify(memberQueryService).getUser(member.getEmail());
		verify(jwtTokenProvider, times(1)).createAccessToken(managed);
		verify(jwtTokenProvider, times(1)).createRefreshToken(managed.getEmail());
	}

	@Test
	void shouldThrowWhenTermsVersionIsInvalid() {
		// given
		Member member = Member.builder()
			.email("oauth-invalid-version@test.com")
			.status(MemberStatus.PENDING_TERMS)
			.build();
		TermsAgreementReq req = new TermsAgreementReq(true, true, "invalid", "2026-08-03");

		ReflectionTestUtils.setField(memberService, "currentTermsVersion", "2026-08-03");
		ReflectionTestUtils.setField(memberService, "currentPrivacyVersion", "2026-08-03");

		// when & then
		assertThatThrownBy(() -> memberService.agreeTerms(member, req))
			.isInstanceOfSatisfying(BusinessException.class, ex ->
				assertThat(ex.getErrorCode()).isEqualTo(MemberErrorCode.INVALID_TERMS_VERSION));

		verifyNoInteractions(jwtTokenProvider);
	}

	@Test
	@DisplayName("프로필 조회 시 회원 정보를 응답으로 변환")
	void shouldReturnProfileFromMember() {
		// given
		Member member = mock(Member.class);
		LocalDateTime createdAt = LocalDateTime.of(2026, 3, 1, 12, 34, 56);
		given(member.getId()).willReturn(1L);
		given(member.getName()).willReturn("Linkiving User");
		given(member.getProfileImageUrl()).willReturn("https://lh3.googleusercontent.com/sample");
		given(member.getEmail()).willReturn("user@example.com");
		given(member.getCreatedAt()).willReturn(createdAt);

		// when
		MemberProfileRes res = memberService.getProfile(member);

		// then
		assertThat(res.id()).isEqualTo(1L);
		assertThat(res.name()).isEqualTo("Linkiving User");
		assertThat(res.profileImageUrl()).isEqualTo("https://lh3.googleusercontent.com/sample");
		assertThat(res.email()).isEqualTo("user@example.com");
		assertThat(res.createdAt()).isEqualTo(createdAt);
	}
}
