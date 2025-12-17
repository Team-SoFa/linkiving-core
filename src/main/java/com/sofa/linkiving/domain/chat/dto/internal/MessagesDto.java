package com.sofa.linkiving.domain.chat.dto.internal;

import java.util.List;

import com.sofa.linkiving.domain.chat.entity.Message;

public record MessagesDto(
	List<Message> messages,
	boolean hasNext
) {
}
