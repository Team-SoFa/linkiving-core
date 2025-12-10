package com.sofa.linkiving.domain.chat.service;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.sofa.linkiving.domain.chat.manager.SubscriptionManager;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
	private final ChatCommandService chatCommandService;
	private final ChatQueryService chatQueryService;

	private final SimpMessagingTemplate messagingTemplate;
	private final SubscriptionManager subscriptionManager;

	private final WebClient webClient = WebClient.create("http://localhost:8080/mock/ai");

	public void generateAnswer(Member member, String userMessage) {

		String email = member.getEmail();

		Disposable subscription = webClient.post()
			.uri("/generate")
			.bodyValue(Map.of("prompt", userMessage))
			.retrieve()
			.bodyToFlux(String.class)
			.doOnComplete(() -> {
				subscriptionManager.remove(email);
				messagingTemplate.convertAndSend("/topic/chat/" + email, "END_OF_STREAM");
			})
			.doOnError(e -> {
				subscriptionManager.remove(email);
				messagingTemplate.convertAndSend("/topic/chat/" + email, "ERROR: " + e.getMessage());
			})
			.subscribe(token -> {
				messagingTemplate.convertAndSend("/topic/chat/" + email, token);
			});

		subscriptionManager.add(email, subscription);
	}

	public void cancelAnswer(Member member) {
		String email = member.getEmail();

		subscriptionManager.cancel(email);

		messagingTemplate.convertAndSend("/topic/chat/" + email, "GENERATION_CANCELLED");
	}
}
