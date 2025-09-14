package com.sofa.linkiving.domain.member.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.MemberRes;
import com.sofa.linkiving.domain.member.service.MemberService;
import com.sofa.linkiving.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberApi {

	private final MemberService memberService;

	@Override
	@PostMapping("/signup")
	public BaseResponse<MemberRes> signup(@Validated SignupReq req) {
		MemberRes signup = memberService.signup(req);

		return BaseResponse.success(signup, "회원 가입에 성공하였습니다.");
	}
}
