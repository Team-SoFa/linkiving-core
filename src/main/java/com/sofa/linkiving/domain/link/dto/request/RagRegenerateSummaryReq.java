package com.sofa.linkiving.domain.link.dto.request;

public record RagRegenerateSummaryReq(
	Long linkId,
	Long userId,
	String url,
	String summary
) {
}
