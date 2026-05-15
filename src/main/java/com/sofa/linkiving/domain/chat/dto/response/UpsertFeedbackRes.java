package com.sofa.linkiving.domain.chat.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sofa.linkiving.global.config.jackson.HashidsSerializer;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpsertFeedbackRes(
	@Schema(description = "피드백 ID")
	@JsonSerialize(using = HashidsSerializer.class)
	Long id
) {
}
