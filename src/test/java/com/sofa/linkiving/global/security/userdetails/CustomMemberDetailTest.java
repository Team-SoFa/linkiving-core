package com.sofa.linkiving.global.security.userdetails;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

public class CustomMemberDetailTest {

	@Test
	@DisplayName("ROLE_ 프리픽스가 포함된 권한 문자열을 부여한다")
	void shouldExposeAuthoritiesWithRolePrefix() {
		// given
		Role role = Role.ADMIN;
		Member member = Member.builder()
			.email("admin@example.com")
			.password("pw")
			.build();
		CustomMemberDetail detail = new CustomMemberDetail(member, role);

		// when
		Collection<? extends GrantedAuthority> authorities = detail.getAuthorities();

		// then
		assertThat(authorities)
			.extracting(GrantedAuthority::getAuthority)
			.containsExactly("ROLE_" + role.name()); // 단일 권한 정책일 경우
	}

	@Test
	@DisplayName("UserDetails username/password가 Member의 email/password와 연결된다")
	void shouldExposeUsernameAndPasswordFromMember() {
		// given
		String email = "test@test.com";
		String password = "test";
		Member member = Member.builder()
			.email(email)
			.password(password)
			.build();
		CustomMemberDetail detail = new CustomMemberDetail(member, member.getRole());

		// when & then
		assertThat(detail.getUsername()).isEqualTo(email);
		assertThat(detail.getPassword()).isEqualTo(password); // 실제 운영에선 인코딩 적용됨
	}

	@Test
	@DisplayName("계정 상태 플래그는 true로 동작한다")
	void shouldReturnTrueForAccountStateFlags() {
		// given
		Member member = Member.builder()
			.email("user@example.com")
			.password("pw")
			.build();
		CustomMemberDetail detail = new CustomMemberDetail(member, member.getRole());

		// when & then
		assertThat(detail.isAccountNonExpired()).isTrue();
		assertThat(detail.isAccountNonLocked()).isTrue();
		assertThat(detail.isCredentialsNonExpired()).isTrue();
		assertThat(detail.isEnabled()).isTrue();
	}
}
