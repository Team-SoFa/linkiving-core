package com.sofa.linkiving.domain.link.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record MetaScrapeReq(
	@Schema(description = "메타 정보를 수집할 URL", example = "https://example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "URL은 필수입니다")
	String url
) {
}
