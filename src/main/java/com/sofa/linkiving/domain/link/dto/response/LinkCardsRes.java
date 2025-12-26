package com.sofa.linkiving.domain.link.dto.response;

import java.util.List;

import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.dto.internal.LinksDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkCardsRes(
	@Schema(description = "링크 목록")
	List<LinkCardRes> links,
	@Schema(description = "다음 페이지 존재 여부")
	boolean hasNext,
	@Schema(description = "마지막 메시지 ID (다음 요청 커서용)")
	Long lastId
) {
	public static LinkCardsRes of(LinksDto linksDto) {
		List<LinkCardRes> links = linksDto.linkDtos().stream().map(LinkCardRes::from).toList();
		Long lastId = links.isEmpty() ? null : links.get(links.size() - 1).id();

		return new LinkCardsRes(links, linksDto.hasNext(), lastId);
	}

	public record LinkCardRes(
		@Schema(description = "링크 ID")
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
}
