package com.sofa.linkiving.security.userdetails;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.service.MemberQueryService;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomMemberService implements UserDetailsService {

	private final MemberQueryService memberQueryService;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		try {
			Member member = memberQueryService.getUser(email);
			return new CustomMemberDetail(member, member.getRole());
		} catch (BusinessException e) {
			throw new UsernameNotFoundException("User not found: " + email, e);
		}
	}
}
