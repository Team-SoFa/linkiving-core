package com.sofa.linkiving.domain.link.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.global.config.jackson.HashidsSerializer;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkCardRes(
	@Schema(description = "링크 ID")
	@JsonSerialize(using = HashidsSerializer.class)
	Long id,

	@Schema(description = "링크 URL", example = "https://example.com")
	String url,

	@Schema(description = "링크 제목", example = "유용한 개발 자료")
	String title,

	@Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
	String imageUrl,

	@Schema(description = "요약 정보")
	String summary
) {
	public static LinkCardRes from(LinkDto dto) {
		return of(dto.link(), dto.summary());
	}

	public static LinkCardRes of(Link link, Summary summary) {
		return new LinkCardRes(
			link.getId(),
			link.getUrl(),
			link.getTitle(),
			link.getImageUrl(),
			summary == null ? null : summary.getContent()
		);
	}
}
