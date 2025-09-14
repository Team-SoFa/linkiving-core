package com.sofa.linkiving.domain.member.controller;

import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.dto.response.MemberRes;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User")
public interface MemberApi {
	@Operation(summary = "회원가입", description = "이메일, 비밀번호를 통한 회원가입을 진행합니다.")
	BaseResponse<MemberRes> signup(SignupReq req);

	@Operation(summary = "로그인", description = "이메일, 비밀번호를 통해 로그인을 진행합니다.")
	BaseResponse<MemberRes> login(LoginReq req);
}
