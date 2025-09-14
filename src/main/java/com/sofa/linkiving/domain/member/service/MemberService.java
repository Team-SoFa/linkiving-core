package com.sofa.linkiving.domain.member.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.MemberRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
	private final MemberCommandService memberCommandService;
	private final MemberQueryService memberQueryService;

	public MemberRes signup(SignupReq req) {

		if (memberQueryService.existsMemberByEmail(req.email())) {
			throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
		}

		// TODO: Change this when Security dependency is added later
		String encoded = Base64.getEncoder()
			.encodeToString(req.password().getBytes(StandardCharsets.UTF_8));

		Member member = memberCommandService.addUser(req.email(), encoded);

		return MemberRes.from(member);
	}

	@Transactional(readOnly = true)
	public MemberRes login(LoginReq req) {
		Member member = memberQueryService.getUser(req.email());

		// TODO: Change this when Security dependency is added later
		String encoded = Base64.getEncoder()
			.encodeToString(req.password().getBytes(StandardCharsets.UTF_8));

		if (!member.verifyPassword(encoded)) {
			throw new BusinessException(MemberErrorCode.INCORRECT_PASSWORD);
		}

		return new MemberRes(member);
	}
}
