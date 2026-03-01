package com.sofa.linkiving.domain.member.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.TokenRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.service.MemberService;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.security.annotation.AuthMember;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
	@PostMapping("/logout")
	public BaseResponse<String> logout(@AuthMember Member member, HttpServletRequest request,
		HttpServletResponse response) {
		memberService.logout(member);
		expireCookie(request, response, "accessToken");
		expireCookie(request, response, "refreshToken");
		return BaseResponse.noContent("로그아웃에 성공하였습니다.");
	}

	private void expireCookie(HttpServletRequest request, HttpServletResponse response, String name) {
		String domain = request.getServerName();
		boolean isLocal = "localhost".equals(domain) || "127.0.0.1".equals(domain);

		ResponseCookie cookie = ResponseCookie.from(name, "")
			.path("/")
			.maxAge(0)
			.httpOnly(!isLocal)
			.secure(!isLocal)
			.sameSite("Lax")
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
