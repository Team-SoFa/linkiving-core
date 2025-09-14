package com.sofa.linkiving.domain.member.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.response.MemberRes;
import com.sofa.linkiving.domain.member.service.MemberService;
import com.sofa.linkiving.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberApi {

	private final MemberService memberService;

	@Override
	@GetMapping("/login")
	public BaseResponse<MemberRes> login(@Validated LoginReq req) {
		MemberRes login = memberService.login(req);

		return BaseResponse.success(login, "로그인에 성공하였습니다.");
	}
}
