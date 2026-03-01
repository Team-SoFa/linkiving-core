package com.sofa.linkiving.domain.member.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.MemberProfileRes;
import com.sofa.linkiving.domain.member.dto.response.TokenRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.service.MemberService;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.security.annotation.AuthMember;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/member")
public class MemberController implements MemberApi {

	private final MemberService memberService;

	@Override
	@PostMapping("/signup")
	public BaseResponse<TokenRes> signup(@RequestBody @Validated SignupReq req) {
		TokenRes signup = memberService.signup(req);

		return BaseResponse.success(signup, "회원 가입에 성공하였습니다.");
	}

	@Override
	@PostMapping("/login")
	public BaseResponse<TokenRes> login(@Validated @RequestBody LoginReq req) {
		TokenRes login = memberService.login(req);

		return BaseResponse.success(login, "로그인에 성공하였습니다.");
	}

	@Override
	@GetMapping("/me")
	public BaseResponse<MemberProfileRes> getProfile(@AuthMember Member member) {
		MemberProfileRes profile = memberService.getProfile(member);
		return BaseResponse.success(profile, "프로필 조회에 성공하였습니다.");
	}
}
