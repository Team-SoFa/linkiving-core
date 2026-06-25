package com.sofa.linkiving.domain.chat.facade;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.chat.ai.TitleClient;
import com.sofa.linkiving.domain.chat.dto.internal.MessagesDto;
import com.sofa.linkiving.domain.chat.dto.response.AnswerRes;
import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.chat.dto.response.CreateChatRes;
import com.sofa.linkiving.domain.chat.dto.response.MessagesRes;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.manager.TaskManager;
import com.sofa.linkiving.domain.chat.service.ChatService;
import com.sofa.linkiving.domain.chat.service.FeedbackService;
import com.sofa.linkiving.domain.chat.service.MessageService;
import com.sofa.linkiving.domain.chat.service.RagChatService;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatFacade {
	private final ChatService chatService;
	private final MessageService messageService;
	private final FeedbackService feedbackService;
	private final RagChatService ragChatService;
	private final TaskManager taskManager;
	private final SimpMessagingTemplate messagingTemplate;
	private final TitleClient titleClient;

	public MessagesRes getMessages(Member member, Long chatId, Long lastId, int size) {
		Chat chat = chatService.getChat(chatId, member);
		MessagesDto result = messageService.getMessages(chat, lastId, size);
		return MessagesRes.of(result.messageDtos(), result.hasNext());
	}

	@Transactional
	public CreateChatRes createChat(String firstChat, Member member) {
		String title = titleClient.generateTitle(firstChat);
		Chat chat = chatService.createChat(title, member);

		return CreateChatRes.from(chat, firstChat);
	}

	public ChatsRes getChats(Member member) {
		List<Chat> chats = chatService.getChats(member);
		return ChatsRes.from(chats);
	}

	@Transactional
	public void deleteChat(Member member, Long chatId) {
		Chat chat = chatService.getChat(chatId, member);

		feedbackService.deleteAll(chat);
		messageService.deleteAll(chat);
		chatService.delete(chat);
	}

	@Transactional
	public void generateAnswer(Long chatId, Member member, String message) {

		CompletableFuture<AnswerRes> task = ragChatService.generateAnswer(chatId, member, message);

		taskManager.put(chatId, task);

		task.whenComplete((result, ex) -> {
			taskManager.remove(chatId);

			if (task.isCancelled() || ex != null) {

				if (ex != null) {
					log.error("AI 답변 생성 중 오류 발생 - chatId: {}, error: {}", chatId, ex.getMessage(), ex);
				} else {
					log.info("AI 답변 생성 작업 취소됨 - chatId: {}", chatId);
				}

				sendNotification(chatId, member, AnswerRes.error(chatId, message));
				return;
			}

			if (result != null) {
				sendNotification(chatId, member, result);
				return;
			}

			log.error("AI 답변 생성 결과가 null 입니다 - chatId: {}", chatId);
			sendNotification(chatId, member, AnswerRes.error(chatId, message));
		});
	}

	private void sendNotification(Long chatId, Member member, AnswerRes res) {
		messagingTemplate.convertAndSendToUser(
			member.getEmail(),
			"/queue/chat",
			res
		);
	}

	public void cancelAnswer(Long chatId, Member member) {
		if (chatService.existsChat(member, chatId)) {
			log.info("Cancelling answer for chat {}", chatId);
			taskManager.cancel(chatId);
		}
	}
}
