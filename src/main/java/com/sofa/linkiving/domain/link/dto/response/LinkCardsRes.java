package com.sofa.linkiving.domain.link.dto.response;

import java.util.List;

import com.sofa.linkiving.domain.link.dto.internal.LinksDto;

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

}
