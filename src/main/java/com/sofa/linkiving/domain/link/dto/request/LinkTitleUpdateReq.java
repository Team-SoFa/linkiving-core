package com.sofa.linkiving.domain.link.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkTitleUpdateReq(
	@Schema(description = "링크 제목", example = "수정된 제목", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "제목은 필수입니다")
	@Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
	String title
) {
}
