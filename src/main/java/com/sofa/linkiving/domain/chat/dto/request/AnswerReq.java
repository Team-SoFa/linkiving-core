package com.sofa.linkiving.domain.chat.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sofa.linkiving.global.config.jackson.HashidsDeserializer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record AnswerReq(
	@Schema(description = "채팅방 ID")
	@JsonDeserialize(using = HashidsDeserializer.class)
	Long chatId,
	@Schema(description = "유저 질문 내용")
	String message,
	@Schema(description = "GA4 client_id")
	@Size(max = 128, message = "clientId는 128자를 초과할 수 없습니다")
	String clientId
) {
}
