package com.sofa.linkiving.domain.link.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sofa.linkiving.domain.link.dto.internal.RagLinkMetadata;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;

import lombok.Builder;

@Builder
public record LinkSyncUpdateReq(
	Long linkId,
	Long userId,
	String url,
	String title,
	String memo,
	String summary,
	@JsonInclude(JsonInclude.Include.NON_NULL) String savedAt,
	@JsonInclude(JsonInclude.Include.NON_NULL) Long savedAtEpochMs,
	@JsonInclude(JsonInclude.Include.NON_NULL) SummaryStatus summaryStatus
) {
	public static LinkSyncUpdateReq from(Link link) {
		return of(link, null);
	}

	public static LinkSyncUpdateReq of(Link link, Summary summary) {
		RagLinkMetadata metadata = RagLinkMetadata.from(link);
		return LinkSyncUpdateReq.builder()
			.linkId(link.getId())
			.userId(link.getMember().getId())
			.url(link.getUrl())
			.title(link.getTitle())
			.memo(link.getMemo())
			.summary(summary != null ? summary.getContent() : null)
			.savedAt(metadata.savedAt())
			.savedAtEpochMs(metadata.savedAtEpochMs())
			.summaryStatus(metadata.summaryStatus())
			.build();
	}
}
