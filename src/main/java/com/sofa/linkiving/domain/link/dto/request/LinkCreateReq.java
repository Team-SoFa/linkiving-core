package com.sofa.linkiving.domain.link.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkCreateReq(
	@Schema(description = "링크 URL", example = "https://example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "URL은 필수입니다")
	@Size(max = 2048, message = "URL은 2048자를 초과할 수 없습니다")
	String url,

	@Schema(description = "링크 제목", example = "유용한 개발 자료", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "제목은 필수입니다")
	@Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
	String title,

	@Schema(description = "메모", example = "나중에 읽어볼 것")
	String memo,

	@Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
	String imageUrl
) {
}
