package com.sofa.linkiving.domain.report.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ReportReq(
	@NotBlank(message = "제보 내용은 필수입니다.")
	@Schema(description = "버그 및 오류 내용")
	String content
) {
}
