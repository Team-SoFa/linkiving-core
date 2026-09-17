package com.sofa.linkiving.domain.link.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sofa.linkiving.domain.link.dto.internal.RagLinkMetadata;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;

public record RagInitialSummaryReq(
	Long linkId,
	Long userId,
	String title,
	String url,
	String memo,
	@JsonInclude(JsonInclude.Include.NON_NULL) String savedAt,
	@JsonInclude(JsonInclude.Include.NON_NULL) Long savedAtEpochMs,
	@JsonInclude(JsonInclude.Include.NON_NULL) SummaryStatus summaryStatus
) {
	public RagInitialSummaryReq(Long linkId, Long userId, String title, String url, String memo) {
		this(linkId, userId, title, url, memo, null, null, null);
	}

	public static RagInitialSummaryReq from(Link link) {
		RagLinkMetadata metadata = RagLinkMetadata.from(link);
		return new RagInitialSummaryReq(link.getId(), link.getMember().getId(), link.getTitle(), link.getUrl(),
			link.getMemo(), metadata.savedAt(), metadata.savedAtEpochMs(), metadata.summaryStatus());
	}
}
