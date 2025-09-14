package com.sofa.linkiving.domain.member.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

public class MemberTest {
	@Test
	void shouldCreateMemberWithValidEmail() {
		// given
		String email = "test@test.com";
		String password = "test";

		// when
		Member member = Member.builder()
			.email(email)
			.password(password)
			.build();

		// then
		assertThat(member.getEmail()).isEqualTo(email);
		assertThat(member.getPassword()).isEqualTo(password);
	}

	@Test
	void shouldThrowExceptionForInvalidEmail() {
		// given
		String invalidEmail = "test";
		String password = "test";

		// when & then
		assertThatThrownBy(() -> Member.builder()
			.email(invalidEmail)
			.password(password)
			.build()
		)
			.isInstanceOfSatisfying(BusinessException.class, ex ->
				assertThat(ex.getErrorCode()).isEqualTo(MemberErrorCode.INVALID_EMAIL_FORMAT)
			);
	}
}
