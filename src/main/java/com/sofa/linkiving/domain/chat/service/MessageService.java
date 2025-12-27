package com.sofa.linkiving.domain.chat.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.sofa.linkiving.domain.chat.dto.internal.MessagesDto;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.chat.manager.SubscriptionManager;

import lombok.RequiredArgsConstructor;
import reactor.core.Disposable;

@Service
@RequiredArgsConstructor
public class MessageService {
	private final MessageCommandService messageCommandService;
	private final MessageQueryService messageQueryService;

	private final SimpMessagingTemplate messagingTemplate;
	private final SubscriptionManager subscriptionManager;

	private final WebClient webClient = WebClient.create("http://localhost:8080/mock/ai");
	private final Map<String, StringBuilder> messageBuffers = new ConcurrentHashMap<>();

	public void deleteAll(Chat chat) {
		messageCommandService.deleteAllByChat(chat);
	}

	public MessagesDto getMessages(Chat chat, Long lastId, int size) {
		return messageQueryService.findAllByChatAndCursor(chat, lastId, size);
	}

	public void generateAnswer(Chat chat, String userMessage) {

		String roomId = chat.getId().toString();

		if (messageBuffers.containsKey(roomId)) {
			return;
		}

		messageBuffers.put(roomId, new StringBuilder());

		Disposable subscription = webClient.post()
			.uri("/generate")
			.bodyValue(Map.of("prompt", userMessage))
			.retrieve()
			.bodyToFlux(String.class)
			.doOnComplete(() -> {
				String fullAnswer = messageBuffers.remove(roomId).toString();

				saveMessage(chat, Type.USER, userMessage);
				saveMessage(chat, Type.AI, fullAnswer);

				subscriptionManager.remove(roomId);
				messagingTemplate.convertAndSend("/topic/chat/" + roomId, "END_OF_STREAM");
			})
			.doOnError(e -> {
				subscriptionManager.remove(roomId);
				messagingTemplate.convertAndSend("/topic/chat/" + roomId, "ERROR: " + e.getMessage());
			})
			.subscribe(token -> {
				StringBuilder buffer = messageBuffers.get(roomId);
				if (buffer != null) {
					buffer.append(token);
				}

				messagingTemplate.convertAndSend("/topic/chat/" + roomId, token);
			});

		subscriptionManager.add(roomId, subscription);
	}

	public void cancelAnswer(Chat chat) {
		String roomId = chat.getId().toString();

		subscriptionManager.cancel(roomId);
		messageBuffers.remove(roomId);

		messagingTemplate.convertAndSend("/topic/chat/" + roomId, "GENERATION_CANCELLED");
	}

	private void saveMessage(Chat chat, Type type, String content) {
		Message message = Message.builder()
			.chat(chat)
			.type(type)
			.content(content)
			.build();

		messageCommandService.saveMessage(message);
	}
}
