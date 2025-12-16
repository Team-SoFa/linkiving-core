package com.sofa.linkiving.domain.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatQueryService {
	private final ChatRepository chatRepository;

	public List<Chat> findAll(Member member) {
		return chatRepository.findAllByMemberOrderByCreatedAtDesc(member);
	}
}
