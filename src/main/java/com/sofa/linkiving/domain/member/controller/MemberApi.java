package com.sofa.linkiving.domain.member.controller;

import com.sofa.linkiving.domain.member.dto.request.MemberWithdrawalReq;
import com.sofa.linkiving.domain.member.dto.request.TermsAgreementReq;
import com.sofa.linkiving.domain.member.dto.response.MemberProfileRes;
import com.sofa.linkiving.domain.member.dto.response.TokenRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "User")
public interface MemberApi {
	@Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화하고 로그아웃 처리합니다.")
	BaseResponse<String> logout(Member member, HttpServletRequest request, HttpServletResponse response);

	@Operation(summary = "회원 탈퇴", description = "회원과 연관된 데이터를 모두 삭제하고 인증 정보를 무효화합니다.")
	BaseResponse<String> withdraw(Member member, MemberWithdrawalReq req, HttpServletRequest request,
		HttpServletResponse response);

	@Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
	BaseResponse<MemberProfileRes> getProfile(Member member);

	@Operation(summary = "약관 동의", description = "OAuth 최초 가입 회원의 필수 약관 동의를 처리합니다.")
	BaseResponse<TokenRes> agreeTerms(Member member, TermsAgreementReq req);
}
