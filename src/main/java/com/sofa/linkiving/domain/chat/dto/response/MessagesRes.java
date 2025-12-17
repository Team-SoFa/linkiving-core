package com.sofa.linkiving.domain.chat.dto.response;

import java.util.Collections;
import java.util.List;

import com.sofa.linkiving.domain.chat.dto.internal.MessageDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MessagesRes(
	@Schema(description = "메시지 목록")
	List<MessageRes> messages,

	@Schema(description = "다음 페이지 존재 여부")
	boolean hasNext,

	@Schema(description = "마지막 메시지 ID (다음 요청 커서용)")
	Long lastId
) {
	public static MessagesRes of(List<MessageDto> messageDtos, boolean hasNext) {
		if (messageDtos.isEmpty()) {
			return new MessagesRes(Collections.emptyList(), hasNext, null);
		}

		List<MessageRes> responses = messageDtos.stream()
			.map(MessageRes::from)
			.toList();

		Long lastId = messageDtos.get(messageDtos.size() - 1).message().getId();

		return new MessagesRes(responses, hasNext, lastId);
	}

}
