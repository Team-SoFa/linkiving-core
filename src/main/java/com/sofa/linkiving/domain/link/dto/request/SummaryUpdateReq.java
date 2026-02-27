package com.sofa.linkiving.domain.link.dto.request;

import com.sofa.linkiving.domain.link.enums.Format;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SummaryUpdateReq(
	@NotNull(message = "요약 내용은 필수입니다.")
	@Schema(description = "요약 내용", example = "새롭게 선택한 요약 내용")
	String summary,
	@NotNull(message = "요약 포맷 정보는 필수입니다.")
	@Schema(description = "요약 포맷 정보 (CONCISE, DETAILED)", example = "CONCISE")
	Format format
) {
}
