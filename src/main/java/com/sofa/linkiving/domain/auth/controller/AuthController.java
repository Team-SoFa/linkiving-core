package com.sofa.linkiving.domain.auth.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.auth.dto.internal.TokenDto;
import com.sofa.linkiving.domain.auth.service.AuthService;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.global.util.CookieUtils;
import com.sofa.linkiving.security.annotation.AuthMember;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {
	private final AuthService authService;
	private final CookieUtils cookieUtils;

	@Override
	@PostMapping("/reissue")
	public BaseResponse<Void> reissue(
		@AuthMember Member member,
		@CookieValue(value = "refreshToken", required = false) String refreshToken,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		TokenDto newTokens = authService.reissue(refreshToken, member);

		cookieUtils.addCookie(request, response, "accessToken", newTokens.accessToken(), newTokens.accessExp());
		cookieUtils.addCookie(request, response, "refreshToken", newTokens.refreshToken(), newTokens.refreshExp());

		return BaseResponse.noContent("토큰 재발급 완료");
	}
}
