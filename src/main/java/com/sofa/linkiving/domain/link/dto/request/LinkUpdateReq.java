package com.sofa.linkiving.domain.link.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record LinkUpdateReq(
	@Schema(description = "링크 제목", example = "유용한 개발 자료")
	@Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
	String title,

	@Schema(description = "메모", example = "나중에 읽어볼 것")
	String memo,

	@Schema(description = "메타데이터 JSON")
	String metadataJson,

	@Schema(description = "태그 (쉼표로 구분)", example = "개발,자료,참고")
	String tags,

	@Schema(description = "중요 여부", example = "true")
	Boolean isImportant
) {
}
