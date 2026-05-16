package com.sofa.linkiving.domain.auth.controller;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "Auth", description = "인증 및 토큰 관리 API")
public interface AuthApi {
	@Operation(summary = "토큰 재발급", description = "쿠키에 저장된 Refresh Token을 검증하여 Access/Refresh Token을 재발급합니다.")
	BaseResponse<Void> reissue(
		Member member,
		@Parameter(description = "리프레시 토큰") String refreshToken,
		HttpServletRequest request,
		HttpServletResponse response
	);
}
