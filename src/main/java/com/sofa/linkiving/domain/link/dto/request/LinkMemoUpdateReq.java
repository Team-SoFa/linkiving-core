package com.sofa.linkiving.domain.link.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkMemoUpdateReq(
	@Schema(description = "메모", example = "수정된 메모 내용")
	String memo
) {
}
