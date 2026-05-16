package com.sofa.linkiving.domain.member.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.config.jackson.HashidsSerializer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MemberProfileRes(
	@Schema(description = "회원 ID")
	@JsonSerialize(using = HashidsSerializer.class)
	Long id,
	@Schema(description = "유저명", example = "Linkiving User")
	String name,
	@Schema(description = "프로필 이미지 URL", example = "https://lh3.googleusercontent.com/...")
	String profileImageUrl,
	@Schema(description = "이메일", example = "user@example.com")
	String email,
	@Schema(description = "가입일", example = "2026-03-01T12:34:56")
	LocalDateTime createdAt
) {
	public static MemberProfileRes from(Member member) {
		return MemberProfileRes.builder()
			.id(member.getId())
			.name(member.getName())
			.profileImageUrl(member.getProfileImageUrl())
			.email(member.getEmail())
			.createdAt(member.getCreatedAt())
			.build();
	}
}
