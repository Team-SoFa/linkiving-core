package com.sofa.linkiving.domain.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageQueryService {
	private final MessageRepository messageRepository;

	public List<Message> findAllByChat(Chat chat) {
		return messageRepository.findAllByChat(chat);
	}
}
