package com.sofa.linkiving.domain.chat.dto.response;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.global.config.jackson.HashidsSerializer;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatsRes(
	@Schema(description = "채팅방 목록")
	List<ChatSummary> chats
) {

	public static ChatsRes from(List<Chat> chatEntities) {
		List<ChatSummary> summaries = chatEntities.stream()
			.map(ChatSummary::from)
			.toList();
		return new ChatsRes(summaries);
	}

	public record ChatSummary(
		@Schema(description = "채팅방 Id")
		@JsonSerialize(using = HashidsSerializer.class)
		Long id,
		@Schema(description = "채팅방 제목")
		String title
	) {
		public static ChatSummary from(Chat chat) {
			return new ChatSummary(
				chat.getId(),
				chat.getTitle()
			);
		}
	}
}
