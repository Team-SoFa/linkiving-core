package com.sofa.linkiving.domain.report.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportReq(
	@NotBlank(message = "제보 내용은 필수입니다.")
	@Schema(description = "버그 및 오류 내용")
	@Size(max = 1000, message = "제보 내용은 1000자를 초과할 수 없습니다")
	String content
) {
}
