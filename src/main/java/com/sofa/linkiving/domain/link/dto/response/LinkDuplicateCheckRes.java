package com.sofa.linkiving.domain.link.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sofa.linkiving.global.config.jackson.HashidsSerializer;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkDuplicateCheckRes(
	@Schema(description = "URL 중복 여부", example = "true")
	boolean exists,

	@Schema(description = "중복된 링크 ID (exists가 true일 때만 반환)", example = "123")
	@JsonSerialize(using = HashidsSerializer.class)
	Long linkId
) {
	public static LinkDuplicateCheckRes notExists() {
		return new LinkDuplicateCheckRes(false, null);
	}

	public static LinkDuplicateCheckRes exists(Long linkId) {
		return new LinkDuplicateCheckRes(true, linkId);
	}
}
