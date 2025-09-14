package com.sofa.linkiving.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.response.MemberRes;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
	private final MemberQueryService memberQueryService;

	@Transactional(readOnly = true)
	public MemberRes login(LoginReq req) {
		Member member = memberQueryService.getUser(req.email());

		if (member.verifyPassword(req.password())) {
			// exception 발생
		}

		return new MemberRes(member);
	}
}
