package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.MemberRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class MemberServiceTest {
	@Mock
	MemberCommandService memberCommandService;
	@InjectMocks
	MemberService memberService;
	@Mock
	private MemberQueryService memberQueryService;

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
	@DisplayName("정상 회원가입 시 비밀번호가 Base64로 인코딩되어 저장")
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

		// when
		MemberRes res = memberService.signup(req);

		// then
		assertThat(res).isNotNull();
		assertThat(res.email()).isEqualTo(req.email());

		// verify
		verify(memberQueryService, times(1)).existsMemberByEmail(email);
		verify(memberCommandService, times(1)).addUser(email, expectedEncoded);
		verifyNoMoreInteractions(memberCommandService, memberQueryService);
	}

	@Test
	@DisplayName("정상 로그인")
	void shouldLoginSuccessfully() {
		// given
		String email = "test@test.com";
		String raw = "test";
		String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
		LoginReq req = new LoginReq(email, raw);

		Member member = Member.builder().email(email).password(encoded).build();
		given(memberQueryService.getUser(email)).willReturn(member);

		// when
		MemberRes res = memberService.login(req);

		// then
		AssertionsForClassTypes.assertThat(res).isNotNull();
		AssertionsForClassTypes.assertThat(res.email()).isEqualTo(email);

		verify(memberQueryService, times(1)).getUser(email);
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
		given(memberQueryService.getUser(email)).willReturn(member);

		// when & then
		assertThatThrownBy(() -> memberService.login(req))
			.isInstanceOfSatisfying(BusinessException.class, ex ->
				AssertionsForClassTypes.assertThat(ex.getErrorCode()).isEqualTo(MemberErrorCode.INCORRECT_PASSWORD)
			);

		verify(memberQueryService, times(1)).getUser(email);
	}
}
