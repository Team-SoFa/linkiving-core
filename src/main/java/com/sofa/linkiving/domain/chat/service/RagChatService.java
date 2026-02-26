package com.sofa.linkiving.domain.chat.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.chat.ai.AnswerClient;
import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.AnswerRes;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Mode;
import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.service.LinkQueryService;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {
	private final AnswerClient answerClient;
	private final MessageCommandService messageCommandService;
	private final MessageQueryService messageQueryService;
	private final LinkQueryService linkQueryService;
	private final ChatQueryService chatQueryService;

	@Async
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public CompletableFuture<AnswerRes> generateAnswer(Long chatId, Member member, String userMessage) {

		Chat chat = chatQueryService.findChat(chatId, member);

		Message question = messageCommandService.saveUserMessage(chat, userMessage);
		List<Message> history = messageQueryService.findTop7ByChatIdAndIdLessThanOrderByIdDesc(
			question.getId(), chat);
		Collections.reverse(history);

		RagAnswerReq request = RagAnswerReq.of(
			member.getId(),
			userMessage,
			history,
			Mode.DETAILED
		);

		RagAnswerRes res = answerClient.generateAnswer(request);

		String fullAnswer = res.answer();

		List<Long> linkIds = parseLinkIds(res.linkIds());
		List<LinkDto> linkDtos = linkQueryService.findAllByIdInWithSummary(linkIds, member);
		List<Link> links = linkDtos.stream().map(LinkDto::link).toList();

		List<String> steps = res.reasoningSteps().stream().map(RagAnswerRes.ReasoningStep::step).toList();

		Message answer = messageCommandService.saveAiMessage(chat, fullAnswer, links);

		AnswerRes payload = AnswerRes.of(chat.getId(), answer, steps, linkDtos);

		return CompletableFuture.completedFuture(payload);

	}

	private List<Long> parseLinkIds(List<String> linkIds) {
		if (linkIds == null || linkIds.isEmpty()) {
			return Collections.emptyList();
		}
		return linkIds.stream()
			.map(id -> {
				try {
					return Long.parseLong(id.trim());
				} catch (NumberFormatException e) {
					log.warn("AI returned invalid linkId: {}", id);
					return null;
				}
			})
			.filter(Objects::nonNull)
			.toList();
	}
}
