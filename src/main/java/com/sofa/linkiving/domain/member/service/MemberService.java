package com.sofa.linkiving.domain.member.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

		// TODO: Change this when Security dependency is added later
		String encoded = Base64.getEncoder()
			.encodeToString(req.password().getBytes(StandardCharsets.UTF_8));

		Member member = memberCommandService.addUser(req.email(), encoded);

		return MemberRes.from(member);
	}
}
