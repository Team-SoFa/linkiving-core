package com.sofa.linkiving.domain.chat.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.error.MessageErrorCode;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageQueryService {
	private final MessageRepository messageRepository;

	public Message findByIdAndMember(Long messageId, Member member) {
		return messageRepository.findByIdAndMember(messageId, member).orElseThrow(
			() -> new BusinessException(MessageErrorCode.MESSAGE_NOT_FOUND)
		);
	}

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
