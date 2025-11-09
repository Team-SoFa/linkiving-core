package com.sofa.linkiving.global.security.userdetails;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.service.MemberQueryService;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;
import com.sofa.linkiving.security.userdetails.CustomMemberService;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CustomMemberServiceTest {

	@Mock
	MemberQueryService memberQueryService;

	@InjectMocks
	CustomMemberService customMemberService;

	@Test
	@DisplayName("이메일로 Member를 조회해 CustomMemberDetail 반환")
	void shouldLoadUserByUsernameAndReturnCustomMemberDetail() {
		String email = "example@example.com";
		String password = "password";

		Member member = Member.builder()
			.email(email)
			.password(password)
			.build();
		given(memberQueryService.getUser(email)).willReturn(member);

		// when
		UserDetails userDetails = customMemberService.loadUserByUsername(email);

		// then
		assertThat(userDetails).isInstanceOf(CustomMemberDetail.class);
		CustomMemberDetail detail = (CustomMemberDetail)userDetails;
		assertThat(detail.getUsername()).isEqualTo(email);
		assertThat(detail.getPassword()).isEqualTo(password);
		assertThat(detail.getAuthorities())
			.extracting(GrantedAuthority::getAuthority)
			.containsExactly("ROLE_" + member.getRole().name());

		// verify
		verify(memberQueryService, times(1)).getUser(email);
		verifyNoMoreInteractions(memberQueryService);
	}

	@Test
	@DisplayName("존재하지 않는 이메일일 경우 UsernameNotFoundException 발생")
	void shouldThrowWhenUserNotFound() {
		// given
		String email = "nope@example.com";
		given(memberQueryService.getUser(email)).willThrow(new UsernameNotFoundException("not found"));

		// when & then
		assertThatThrownBy(() -> customMemberService.loadUserByUsername(email))
			.isInstanceOf(UsernameNotFoundException.class);

		// verify
		verify(memberQueryService).getUser(email);
	}
}
