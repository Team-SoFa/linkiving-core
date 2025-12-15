package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageCommandService {
	private final MessageRepository messageRepository;

	public void deleteAllByChat(Chat chat) {
		messageRepository.deleteAllByChat(chat);
	}

	public Message saveMessage(Message message) {
		return messageRepository.save(message);
	}
}
