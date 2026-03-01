package com.sofa.linkiving.domain.member.controller;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.TokenRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "User")
public interface MemberApi {
	@Operation(summary = "회원가입", description = "이메일, 비밀번호를 통한 회원가입을 진행합니다.")
	BaseResponse<TokenRes> signup(SignupReq req);

	@Operation(summary = "로그인", description = "이메일, 비밀번호를 통해 로그인을 진행합니다.")
	BaseResponse<TokenRes> login(LoginReq req);

	@Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화하고 로그아웃 처리합니다.")
	BaseResponse<String> logout(Member member, HttpServletRequest request, HttpServletResponse response);
}
