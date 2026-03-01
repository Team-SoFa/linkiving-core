package com.sofa.linkiving.domain.member.dto.response;

import java.time.LocalDateTime;

import com.sofa.linkiving.domain.member.entity.Member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MemberProfileRes(
	@Schema(description = "회원 ID", example = "1")
	Long id,
	@Schema(description = "이메일", example = "user@example.com")
	String email,
	@Schema(description = "가입일", example = "2026-03-01T12:34:56")
	LocalDateTime createdAt
) {
	public static MemberProfileRes from(Member member) {
		return MemberProfileRes.builder()
			.id(member.getId())
			.email(member.getEmail())
			.createdAt(member.getCreatedAt())
			.build();
	}
}
