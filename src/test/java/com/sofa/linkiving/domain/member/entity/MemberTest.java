package com.sofa.linkiving.domain.member.entity;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.Test;

public class MemberTest {
	@Test
	void shouldCreateMemberWithValidEmail() {
		// given
		String email = "test@example.com";
		String password = "password123";

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
		String invalidEmail = "invalid-email";
		String password = "password123";

		// when & then
		assertThatThrownBy(() -> Member.builder()
			.email(invalidEmail)
			.password(password)
			.build()
		)
			.isInstanceOf(IllegalArgumentException.class);
	}
}
