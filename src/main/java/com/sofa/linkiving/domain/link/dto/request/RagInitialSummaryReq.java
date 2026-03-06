package com.sofa.linkiving.domain.link.dto.request;

public record RagInitialSummaryReq(
	Long linkId,
	Long userId,
	String title,
	String url,
	String memo
) {
}
