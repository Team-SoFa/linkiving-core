package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.request.MemberWithdrawalReq;
import com.sofa.linkiving.domain.member.dto.request.SignupReq;
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
	MemberCommandService memberCommandService;
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
	@DisplayName("중복된 이메일로 회원가입 시 예외 발생")
	void shouldThrowBusinessExceptionWhenEmailAlreadyExists() {
		// given
		String email = "test@example.com";
		String password = "plainPassword";
		SignupReq req = new SignupReq(email, password);

		when(memberQueryService.existsMemberByEmail(email)).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> memberService.signup(req))
			.isInstanceOfSatisfying(BusinessException.class, ex ->
				assertThat(ex.getErrorCode()).isEqualTo(MemberErrorCode.DUPLICATE_EMAIL)
			);

		// then: addUser는 호출되지 않아야 함
		verify(memberQueryService, times(1)).existsMemberByEmail(email);
		verifyNoInteractions(memberCommandService);
	}

	@Test
	@DisplayName("정상 회원가입 시 비밀번호가 Base64로 인코딩되어 저장되고 토큰 반환Z")
	void shouldSignupAndEncodePassword() {
		// given
		String email = "test@test.com";
		String password = "test";
		SignupReq req = new SignupReq(email, password);

		String expectedEncoded = Base64.getEncoder()
			.encodeToString(req.password().getBytes(StandardCharsets.UTF_8));

		Member saved = Member.builder()
			.email(req.email())
			.password(expectedEncoded)
			.build();

		when(memberQueryService.existsMemberByEmail(email)).thenReturn(false);
		when(memberCommandService.addUser(eq(req.email()), eq(expectedEncoded)))
			.thenReturn(saved);

		given(jwtTokenProvider.createAccessToken(any(Member.class))).willReturn("mock-access-token");
		given(jwtTokenProvider.createRefreshToken(any())).willReturn("mock-refresh-token");

		// when
		TokenRes res = memberService.signup(req);

		// then
		assertThat(res).isNotNull();
		assertThat(res.accessToken()).isNotNull();
		assertThat(res.refreshToken()).isNotNull();

		// verify
		verify(memberQueryService, times(1)).existsMemberByEmail(email);
		verify(memberCommandService, times(1)).addUser(email, expectedEncoded);
		verifyNoMoreInteractions(memberCommandService, memberQueryService);
	}

	@Test
	@DisplayName("정상 로그인 시 토큰 반환")
	void shouldLoginSuccessfully() {
		// given
		String email = "test@test.com";
		String raw = "test";
		String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
		LoginReq req = new LoginReq(email, raw);

		Member member = Member.builder().email(email).password(encoded).build();
		given(memberQueryService.getUserForUpdate(email)).willReturn(member);

		given(jwtTokenProvider.createAccessToken(any(Member.class))).willReturn("mock-access-token");
		given(jwtTokenProvider.createRefreshToken(any())).willReturn("mock-refresh-token");

		// when
		TokenRes res = memberService.login(req);

		// then
		assertThat(res).isNotNull();
		assertThat(res.accessToken()).isNotNull();
		assertThat(res.refreshToken()).isNotNull();

		verify(memberQueryService, times(1)).getUserForUpdate(email);
	}

	@Test
	@DisplayName("잘못된 비밀번호로 로그인 시 INCORRECT_PASSWORD 에러코드로 예외 발생")
	void shouldThrowIncorrectPasswordErrorCodeWhenPasswordNotMatch() {
		// given
		String email = "test@test.com";
		String correct = "correctPassword";
		String incorrect = "incorrectPassword";
		String encodedCorrect = Base64.getEncoder().encodeToString(correct.getBytes(StandardCharsets.UTF_8));

		LoginReq req = new LoginReq(email, incorrect);

		Member member = Member.builder().email(email).password(encodedCorrect).build();
		given(memberQueryService.getUserForUpdate(email)).willReturn(member);

		// when & then
		assertThatThrownBy(() -> memberService.login(req))
			.isInstanceOfSatisfying(BusinessException.class, ex ->
				AssertionsForClassTypes.assertThat(ex.getErrorCode()).isEqualTo(MemberErrorCode.INCORRECT_PASSWORD)
			);

		verify(memberQueryService, times(1)).getUserForUpdate(email);
	}

	@Test
	@DisplayName("로그아웃 시 Redis에 저장된 refresh token 삭제")
	void shouldDeleteRefreshTokenOnLogout() {
		// given
		Member member = Member.builder().email("test@test.com").password("pw").build();

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
			.password("pw")
			.status(MemberStatus.PENDING_TERMS)
			.build();
		Member managed = Member.builder()
			.email("oauth@test.com")
			.password("pw")
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
			.password("pw")
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
