package com.sofa.linkiving.domain.link.dto.request;

import com.sofa.linkiving.domain.link.enums.Format;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record RegenerateSummaryReq(
	@Schema(description = "요청 형식(CONCISE: 간결하게, DETAILED:자세하게)")
	Format format
) {
}
