package com.sofa.linkiving.domain.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {
	private final ChatCommandService chatCommandService;
	private final ChatQueryService chatQueryService;

	public Chat getChat(Long chatId, Member member) {
		return chatQueryService.findChat(chatId, member);
	}

	public void delete(Chat chat) {
		chatCommandService.deleteChat(chat);
	}

	public List<Chat> getChats(Member member) {
		return chatQueryService.findAllOrderByLastMessageDesc(member);
	}

	public Chat createChat(String title, Member member) {
		return chatCommandService.saveChat(title, member);
	}

	public boolean existsChat(Member member, Long chatId) {
		return chatQueryService.existsByIdAndMember(member, chatId);
	}
}
