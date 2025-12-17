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

	public List<Chat> getChats(Member member) {
		return chatQueryService.findAllOrderByLastMessageDesc(member);
	}
}
