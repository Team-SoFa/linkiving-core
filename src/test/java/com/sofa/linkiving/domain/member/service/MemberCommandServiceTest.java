package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MemberCommandServiceTest {

	@Mock
	MemberRepository memberRepository;

	@InjectMocks
	MemberCommandService memberCommandService;

	@Test
	@DisplayName("OAuth 회원이 이미 존재하면 name, profileImageUrl을 갱신한다")
	void shouldUpdateExistingOAuthMemberProfile() {
		// given
		String email = "oauth@test.com";
		Member existing = Member.builder()
			.email(email)
			.name("old-name")
			.profileImageUrl("https://old.example.com/profile.png")
			.build();

		when(memberRepository.findByEmailForUpdate(email)).thenReturn(Optional.of(existing));

		// when
		Member result = memberCommandService.createOrUpdate(
			email,
			"new-name",
			"https://new.example.com/profile.png"
		);

		// then
		assertThat(result).isSameAs(existing);
		assertThat(result.getName()).isEqualTo("new-name");
		assertThat(result.getProfileImageUrl()).isEqualTo("https://new.example.com/profile.png");

		verify(memberRepository, times(1)).findByEmailForUpdate(email);
		verify(memberRepository, never()).save(any(Member.class));
		verifyNoMoreInteractions(memberRepository);
	}

	@Test
	@DisplayName("OAuth 신규 회원이면 name, profileImageUrl을 저장한다")
	void shouldCreateOAuthMemberWithProfile() {
		// given
		String email = "oauth-new@test.com";
		when(memberRepository.findByEmailForUpdate(email)).thenReturn(Optional.empty());
		when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		Member result = memberCommandService.createOrUpdate(
			email,
			"google-name",
			"https://googleusercontent.com/profile.png"
		);

		// then
		assertThat(result.getEmail()).isEqualTo(email);
		assertThat(result.getName()).isEqualTo("google-name");
		assertThat(result.getProfileImageUrl()).isEqualTo("https://googleusercontent.com/profile.png");

		verify(memberRepository, times(1)).findByEmailForUpdate(email);
		verify(memberRepository, times(1)).save(any(Member.class));
		verifyNoMoreInteractions(memberRepository);
	}

	@Test
	@DisplayName("OAuth 신규 회원 생성 시 PENDING_TERMS 상태와 USER 권한으로 저장함")
	void shouldSavePendingTermsStatusWhenCreateNewOAuthMember() {
		// given
		String email = "oauth-status@test.com";
		given(memberRepository.findByEmailForUpdate(email)).willReturn(Optional.empty());
		given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		Member result = memberCommandService.createOrUpdate(email, "google-name", null);

		// then
		assertThat(result.getStatus()).isEqualTo(MemberStatus.PENDING_TERMS);
		assertThat(result.getRole()).isEqualTo(Role.USER);
	}
}
