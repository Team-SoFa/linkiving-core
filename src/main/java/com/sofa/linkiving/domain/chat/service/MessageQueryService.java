package com.sofa.linkiving.domain.chat.service;

import java.util.List;

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

	public List<Message> findAllByChat(Chat chat) {
		return messageRepository.findAllByChat(chat);
	}
}
