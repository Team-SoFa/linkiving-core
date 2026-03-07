package com.sofa.linkiving.domain.link.dto.request;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;

import lombok.Builder;

@Builder
public record LinkSyncUpdateReq(
	Long linkId,
	Long userId,
	String url,
	String title,
	String memo,
	String summary
) {
	public static LinkSyncUpdateReq from(Link link) {
		return LinkSyncUpdateReq.builder()
			.linkId(link.getId())
			.userId(link.getMember().getId())
			.url(link.getUrl())
			.title(link.getTitle())
			.memo(link.getMemo())
			.build();
	}

	public static LinkSyncUpdateReq of(Link link, Summary summary) {
		return LinkSyncUpdateReq.builder()
			.linkId(link.getId())
			.userId(link.getMember().getId())
			.url(link.getUrl())
			.title(link.getTitle())
			.memo(link.getMemo())
			.summary(summary != null ? summary.getContent() : null)
			.build();
	}
}
