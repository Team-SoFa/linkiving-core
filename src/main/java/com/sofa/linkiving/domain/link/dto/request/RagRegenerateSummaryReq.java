package com.sofa.linkiving.domain.link.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sofa.linkiving.domain.link.dto.internal.RagLinkMetadata;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;

public record RagRegenerateSummaryReq(
	Long linkId,
	Long userId,
	String url,
	String summary,
	@JsonInclude(JsonInclude.Include.NON_NULL) String savedAt,
	@JsonInclude(JsonInclude.Include.NON_NULL) Long savedAtEpochMs,
	@JsonInclude(JsonInclude.Include.NON_NULL) SummaryStatus summaryStatus
) {
	public RagRegenerateSummaryReq(Long linkId, Long userId, String url, String summary) {
		this(linkId, userId, url, summary, null, null, null);
	}

	public static RagRegenerateSummaryReq of(Link link, String summary) {
		RagLinkMetadata metadata = RagLinkMetadata.from(link);
		return new RagRegenerateSummaryReq(link.getId(), link.getMember().getId(), link.getUrl(), summary,
			metadata.savedAt(), metadata.savedAtEpochMs(), metadata.summaryStatus());
	}
}
