package com.sofa.linkiving.domain.chat.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
import com.sofa.linkiving.global.analytics.Ga4Event;
import com.sofa.linkiving.global.analytics.Ga4Publisher;
import com.sofa.linkiving.global.logging.LogContext;

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
	private final Ga4Publisher ga4Publisher;

	@Async("aiTaskExecutor")
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public CompletableFuture<AnswerRes> generateAnswer(Long chatId, Member member, String userMessage) {
		return generateAnswer(chatId, member, userMessage, null);
	}

	@Async("aiTaskExecutor")
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public CompletableFuture<AnswerRes> generateAnswer(
		Long chatId,
		Member member,
		String userMessage,
		String clientId
	) {
		String queryId = UUID.randomUUID().toString();
		long startNanos = System.nanoTime();

		try (LogContext.MdcScope memberScope = LogContext.withMemberId(member.getId());
			LogContext.MdcScope chatScope = LogContext.withChatId(chatId)) {

			if (hasAnalyticsClient(clientId)) {
				long linkCountAtQuery = linkQueryService.countByMemberAndIsDeleteFalse(member);
				publishQuerySubmit(member, clientId, queryId, linkCountAtQuery);
			}

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

			Message answer = messageCommandService.saveAiMessage(chat, fullAnswer, queryId, links);

			AnswerRes payload = AnswerRes.of(chat.getId(), answer, steps, linkDtos);
			publishQueryResponseComplete(member, clientId, queryId, startNanos, false, null, res, linkDtos);

			return CompletableFuture.completedFuture(payload);
		} catch (RuntimeException exception) {
			publishQueryResponseComplete(member, clientId, queryId, startNanos, true,
				exception.getClass().getSimpleName(), null, List.of());
			throw exception;
		}

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

	private void publishQuerySubmit(Member member, String clientId, String queryId, long linkCountAtQuery) {
		publishQueryEvent(member, clientId, "query_submit", Map.of(
			"query_id", queryId,
			"link_count_at_query", linkCountAtQuery
		));
	}

	private void publishQueryResponseComplete(
		Member member,
		String clientId,
		String queryId,
		long startNanos,
		boolean isError,
		String errorType,
		RagAnswerRes ragAnswer,
		List<LinkDto> selectedLinks
	) {
		Map<String, Object> params = new HashMap<>();
		params.put("query_id", queryId);
		params.put("is_error", isError);
		params.put("latency_ms", elapsedMillis(startNanos));

		if (ragAnswer != null) {
			params.put("selected_count", firstNonNull(ragAnswer.selectedCount(), selectedLinks.size()));
			putIfPresent(params, "retrieved_count", ragAnswer.retrievedCount());
			putIfPresent(params, "top_similarity", ragAnswer.topSimilarity());
		}
		putIfPresent(params, "error_type", errorType);

		publishQueryEvent(member, clientId, "query_response_complete", params);
	}

	private void publishQueryEvent(Member member, String clientId, String eventName, Map<String, Object> params) {
		if (!hasAnalyticsClient(clientId)) {
			return;
		}
		String userId = member.getId() == null ? null : String.valueOf(member.getId());
		ga4Publisher.publish(clientId, userId, new Ga4Event(eventName, params));
	}

	private boolean hasAnalyticsClient(String clientId) {
		return clientId != null && !clientId.isBlank();
	}

	private void putIfPresent(Map<String, Object> params, String key, Object value) {
		if (value != null) {
			params.put(key, value);
		}
	}

	private Integer firstNonNull(Integer value, int fallback) {
		return value == null ? fallback : value;
	}

	private long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}
}
