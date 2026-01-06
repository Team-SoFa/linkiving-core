package com.sofa.linkiving.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnswerReq(
	@Schema(description = "채팅방 ID")
	Long chatId,
	@Schema(description = "유저 질문 내용")
	String message
) {
}
