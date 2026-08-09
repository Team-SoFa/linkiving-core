package com.sofa.linkiving.domain.member.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.member.enums.MemberStatus;
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

	@Test
	void shouldCreatePendingTermsMemberAndActivateAfterAgreement() {
		// given
		Member member = Member.builder()
			.email("oauth@test.com")
			.password("oauth@test.com")
			.status(MemberStatus.PENDING_TERMS)
			.build();

		// when
		member.agreeTerms("2026-08-03", "2026-08-03");

		// then
		assertThat(member.needsTermsAgreement()).isFalse();
		assertThat(member.hasAgreedTerms()).isTrue();
		assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		assertThat(member.getTermsVersion()).isEqualTo("2026-08-03");
		assertThat(member.getPrivacyVersion()).isEqualTo("2026-08-03");
		assertThat(member.getTermsAgreedAt()).isNotNull();
		assertThat(member.getPrivacyAgreedAt()).isNotNull();
	}
}
