package com.sofa.linkiving.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AddFeedbackRes(
	@Schema(description = "피드백 ID")
	Long id
) {
}
