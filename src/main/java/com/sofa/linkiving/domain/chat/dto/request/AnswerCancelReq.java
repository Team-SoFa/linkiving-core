package com.sofa.linkiving.domain.chat.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sofa.linkiving.global.config.jackson.HashidsDeserializer;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnswerCancelReq(
	@Schema(description = "채팅방 ID")
	@JsonDeserialize(using = HashidsDeserializer.class)
	Long chatId
) {
}
