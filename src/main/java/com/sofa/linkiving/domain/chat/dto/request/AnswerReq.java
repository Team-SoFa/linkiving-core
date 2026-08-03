package com.sofa.linkiving.domain.chat.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sofa.linkiving.global.config.jackson.HashidsDeserializer;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnswerReq(
	@Schema(description = "Chat ID")
	@JsonDeserialize(using = HashidsDeserializer.class)
	Long chatId,
	@Schema(description = "User message")
	String message,
	@Schema(description = "GA4 client_id")
	String clientId
) {
}
