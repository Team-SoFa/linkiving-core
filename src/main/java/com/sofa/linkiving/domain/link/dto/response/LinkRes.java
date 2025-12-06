package com.sofa.linkiving.domain.link.dto.response;

import java.time.LocalDateTime;

import com.sofa.linkiving.domain.link.entity.Link;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkRes(
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

	@Schema(description = "메타데이터 JSON")
	String metadataJson,

	@Schema(description = "태그 (쉼표로 구분)", example = "개발,자료,참고")
	String tags,

	@Schema(description = "중요 여부")
	boolean isImportant,

	@Schema(description = "생성 일시")
	LocalDateTime createdAt,

	@Schema(description = "수정 일시")
	LocalDateTime updatedAt
) {
	public static LinkRes from(Link link) {
		return new LinkRes(
			link.getId(),
			link.getUrl(),
			link.getTitle(),
			link.getMemo(),
			link.getImageUrl(),
			link.getMetadataJson(),
			link.getTags(),
			link.isImportant(),
			link.getCreatedAt(),
			link.getUpdatedAt()
		);
	}
}
