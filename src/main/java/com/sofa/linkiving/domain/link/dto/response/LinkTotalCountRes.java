package com.sofa.linkiving.domain.link.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkTotalCountRes(
	@Schema(description = "링크 전체 개수")
	Long totalCount
) {
}
