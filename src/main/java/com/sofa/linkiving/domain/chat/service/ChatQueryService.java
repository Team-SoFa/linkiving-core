package com.sofa.linkiving.domain.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.error.ChatErrorCode;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatQueryService {
	private final ChatRepository chatRepository;

	public Chat findChat(Long chatId, Member member) {
		return chatRepository.findByIdAndMember(chatId, member).orElseThrow(
			() -> new BusinessException(ChatErrorCode.CHAT_NOT_FOUND)
		);
	}

	public List<Chat> findAllOrderByLastMessageDesc(Member member) {
		return chatRepository.findAllByMemberOrderByLastMessageDesc(member);
	}

	public boolean existsByIdAndMember(Member member, Long chatId) {
		return chatRepository.existsByIdAndMember(chatId, member);
	}
}
