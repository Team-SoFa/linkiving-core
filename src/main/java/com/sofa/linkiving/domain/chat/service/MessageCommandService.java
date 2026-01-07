package com.sofa.linkiving.domain.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.link.entity.Link;

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

	public Message saveUserMessage(Chat chat, String content) {
		Message message = Message.builder()
			.chat(chat)
			.type(Type.USER)
			.content(content)
			.build();

		return messageRepository.save(message);
	}

	public Message saveAiMessage(Chat chat, String content, List<Link> links) {
		Message message = Message.builder()
			.chat(chat)
			.type(Type.AI)
			.content(content)
			.links(links)
			.build();

		return messageRepository.save(message);
	}
}
