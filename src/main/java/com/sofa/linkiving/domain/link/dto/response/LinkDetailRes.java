package com.sofa.linkiving.domain.link.dto.response;

import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkDetailRes(
	@Schema(description = "링크 ID")
	Long id,

	@Schema(description = "링크 URL", example = "https://example.com")
	String url,

	@Schema(description = "링크 제목", example = "유용한 개발 자료")
	String title,

	@Schema(description = "메모", example = "나중에 읽어볼 것")
	String memo,

	@Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
	String imageUrl,

	@Schema(description = "요약 정보")
	SummaryRes summary
) {
	public static LinkDetailRes from(LinkDto dto) {
		return of(dto.link(), dto.summary());
	}

	public static LinkDetailRes of(Link link, Summary summary) {
		return new LinkDetailRes(
			link.getId(),
			link.getUrl(),
			link.getTitle(),
			link.getMemo(),
			link.getImageUrl(),
			SummaryRes.from(summary)
		);
	}

}
