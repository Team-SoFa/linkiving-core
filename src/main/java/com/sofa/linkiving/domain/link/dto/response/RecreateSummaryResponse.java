package com.sofa.linkiving.domain.link.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record RecreateSummaryResponse(
	@Schema(description = "기존 요약")
	String existingSummary,
	@Schema(description = "신규 요약")
	String newSummary,
	@Schema(description = "비교 정보")
	String comparison
) {
}
