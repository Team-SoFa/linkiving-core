package com.sofa.linkiving.domain.member.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberQueryService {
	private final MemberRepository memberRepository;

	boolean existsMemberByEmail(String email) {
		return memberRepository.existsMemberByEmail(email);
	}

	public Member getUser(String email) {

		return memberRepository.findByEmail(email).orElseThrow(
			() -> new BusinessException(MemberErrorCode.USER_NOT_FOUND)
		);
	}
}
