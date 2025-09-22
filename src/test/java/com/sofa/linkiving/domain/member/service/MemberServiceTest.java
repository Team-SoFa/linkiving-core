package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.MemberRes;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

	@Mock
	MemberCommandService memberCommandService;

	@InjectMocks
	MemberService memberService;

	@Test
	@DisplayName("회원가입 시 비밀번호가 Base64로 인코딩되어 저장")
	void shouldSignupAndEncodePassword() {
		// given
		SignupReq req = new SignupReq("user@example.com", "testPassword");
		String expectedEncoded = Base64.getEncoder()
			.encodeToString(req.password().getBytes(StandardCharsets.UTF_8));

		Member saved = Member.builder()
			.email(req.email())
			.password(expectedEncoded)
			.build();

		when(memberCommandService.addUser(eq(req.email()), eq(expectedEncoded)))
			.thenReturn(saved);

		// when
		MemberRes res = memberService.signup(req);

		// then
		assertThat(res).isNotNull();
		assertThat(res.email()).isEqualTo(req.email());

		// verify interaction
		verify(memberCommandService, times(1)).addUser(eq(req.email()), eq(expectedEncoded));
		verifyNoMoreInteractions(memberCommandService);
	}
}
