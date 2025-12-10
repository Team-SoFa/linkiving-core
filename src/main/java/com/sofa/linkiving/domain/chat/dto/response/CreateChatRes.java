package com.sofa.linkiving.domain.chat.dto.response;

import com.sofa.linkiving.domain.chat.entity.Chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateChatRes(
	@Schema(description = "채팅방 ID")
	Long id,
	@Schema(description = "채팅방 제목")
	String title,
	@Schema(description = "최초 대화")
	String firstChat
) {
	public static CreateChatRes from(Chat chat, String firstChat) {
		return CreateChatRes.builder()
			.id(chat.getId())
			.title(chat.getTitle())
			.firstChat(firstChat)
			.build();
	}
}
