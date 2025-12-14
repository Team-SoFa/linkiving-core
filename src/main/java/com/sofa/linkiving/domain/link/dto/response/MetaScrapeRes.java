package com.sofa.linkiving.domain.link.dto.response;

import com.sofa.linkiving.domain.link.dto.OgTagDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MetaScrapeRes(
	@Schema(description = "페이지 제목", example = "Example Domain")
	String title,

	@Schema(description = "페이지 설명", example = "This domain is for use in illustrative examples...")
	String description,

	@Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
	String image,

	@Schema(description = "페이지 URL", example = "https://example.com")
	String url
) {
	public static MetaScrapeRes from(OgTagDto ogTag) {
		return new MetaScrapeRes(
			ogTag.title(),
			ogTag.description(),
			ogTag.image(),
			ogTag.url()
		);
	}
}
