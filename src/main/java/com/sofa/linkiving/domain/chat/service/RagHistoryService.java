package com.sofa.linkiving.domain.chat.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq.RagHistoryLinkReq;
import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq.RagMessageReq;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RagHistoryService {

	private static final int MESSAGE_LIMIT = 7;
	private static final int LINKS_PER_MESSAGE = 10;
	private static final int TOTAL_LINK_LIMIT = 20;
	private final MessageRepository messageRepository;

	@Transactional(readOnly = true)
	public List<RagMessageReq> findHistory(Long beforeId, Chat chat, Member member) {
		List<Message> messages = messageRepository.findRagHistory(chat, member, beforeId,
			PageRequest.of(0, MESSAGE_LIMIT));
		if (messages.isEmpty()) {
			return List.of();
		}
		Map<Long, Map<Long, RagHistoryLinkReq>> groupedLinks = new LinkedHashMap<>();
		List<Long> aiMessageIds = messages.stream().filter(message -> message.getType() == Type.AI)
			.map(Message::getId).toList();
		if (!aiMessageIds.isEmpty()) {
			for (var row : messageRepository.findRagHistoryLinks(aiMessageIds, chat, member)) {
				groupedLinks.computeIfAbsent(row.messageId(), ignored -> new LinkedHashMap<>())
					.putIfAbsent(row.link().getId(), RagHistoryLinkReq.from(row.link(), row.summary()));
			}
		}
		List<RagMessageReq> history = new ArrayList<>();
		int remaining = TOTAL_LINK_LIMIT;
		// 최근 답변부터 예산을 배정한 뒤 대화 순서로 반환한다.
		for (Message message : messages) {
			var available = groupedLinks.getOrDefault(message.getId(), Map.of()).values();
			List<RagHistoryLinkReq> links = available.stream().limit(Math.min(LINKS_PER_MESSAGE, remaining)).toList();
			remaining -= links.size();
			history.add(RagMessageReq.from(message, links, links.size() < available.size()));
		}
		Collections.reverse(history);
		return List.copyOf(history);
	}
}
