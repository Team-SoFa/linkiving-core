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

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
public class MemberQueryServiceTest {
	@Mock
	MemberRepository memberRepository;

	@InjectMocks
	MemberQueryService memberQueryService;

	@Test
	@DisplayName("이메일로 회원 조회")
	void shouldGetUserByEmail() {
		// given
		String email = "test@test.com";
		String password = "test";
		Member member = Member.builder().email(email).password(password).build();
		given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

		// when
		Member result = memberQueryService.getUser(email);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getEmail()).isEqualTo(email);
	}

	@Test
	@DisplayName("이메일로 회원 조회 실패 시 예외 발생")
	void shouldThrowWhenUserNotFound() {
		//given
		String email = "test@test.com";
		given(memberRepository.findByEmail(email)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> memberQueryService.getUser(email))
			.isInstanceOfSatisfying(BusinessException.class,
				ex -> assertThat(ex.getErrorCode()).isEqualTo(MemberErrorCode.USER_NOT_FOUND)
			);
	}

}
