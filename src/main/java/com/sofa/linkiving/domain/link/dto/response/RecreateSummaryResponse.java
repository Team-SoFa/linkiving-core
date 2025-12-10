package com.sofa.linkiving.domain.link.dto.response;

import lombok.Builder;

@Builder
public record RecreateSummaryResponse(
	String existingSummary,
	String newSummary,
	String comparison
) {
}
