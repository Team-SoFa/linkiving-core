package com.sofa.linkiving.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.MemberRes;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
	private final MemberCommandService memberCommandService;

	public MemberRes signup(SignupReq req) {
		Member member = memberCommandService.addUser(req.email(), req.password());

		return MemberRes.from(member);
	}
}
