package com.sofa.linkiving.domain.chat.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageQueryService {
	private final MessageRepository messageRepository;

	public Slice<Message> findAllByChatAndCursor(Chat chat, Long lastId, int size) {
		PageRequest pageRequest = PageRequest.of(0, size + 1);
		List<Message> messages = messageRepository.findAllByChatAndCursor(chat, lastId, pageRequest);

		boolean hasNext = false;
		if (size < messages.size()) {
			hasNext = true;
			messages.remove(size);
		}

		return new SliceImpl<>(messages, pageRequest, hasNext);
	}
}
