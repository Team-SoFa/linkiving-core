package com.sofa.linkiving.domain.chat.dto.internal;

import java.util.List;

public record MessagesDto(
	List<MessageDto> messageDtos,
	boolean hasNext
) {
}
