package com.sofa.linkiving.domain.member.dto.response;

import com.sofa.linkiving.domain.member.entity.Member;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberRes(
	@Schema(description = "유저 고유 번호")
	Long userId,
	@Schema(description = "유저 이메일")
	String email
) {
	public MemberRes(Member member) {
		this(member.getId(), member.getEmail());
	}
}
