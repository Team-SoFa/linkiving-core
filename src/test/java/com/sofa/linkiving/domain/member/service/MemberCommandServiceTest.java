package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

	@Mock
	MemberRepository memberRepository;

	@InjectMocks
	MemberCommandService memberCommandService;

	@Test
	@DisplayName("유효한 이메일과 인코딩된 비밀번호로 회원 생성")
	void shouldCreateMemberWithValidEmailAndPassword() {
		// given
		String email = "test@example.com";
		String encoded = "encodedPW";

		Member saved = Member.builder()
			.email(email)
			.password(encoded)
			.build();

		when(memberRepository.save(any(Member.class))).thenReturn(saved);

		// when
		Member result = memberCommandService.addUser(email, encoded);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getEmail()).isEqualTo(email);
		assertThat(result.getPassword()).isEqualTo(encoded);

		// verify interaction
		ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
		verify(memberRepository, times(1)).save(captor.capture());
		verifyNoMoreInteractions(memberRepository);

		Member captured = captor.getValue();
		assertThat(captured.getEmail()).isEqualTo(email);
		assertThat(captured.getPassword()).isEqualTo(encoded);
	}
}
