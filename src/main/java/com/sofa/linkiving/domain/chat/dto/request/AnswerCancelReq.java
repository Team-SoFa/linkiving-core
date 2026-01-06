package com.sofa.linkiving.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnswerCancelReq(
	@Schema(description = "채팅방 ID")
	Long chatId
) {
}
