package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatCommandService {
	private final ChatRepository chatRepository;

	public Chat saveChat(String title, Member member) {
		return chatRepository.save(
			Chat.builder()
				.member(member)
				.title(title)
				.build()
		);
	}

	public void deleteChat(Chat chat) {
		chatRepository.delete(chat);
	}
}
