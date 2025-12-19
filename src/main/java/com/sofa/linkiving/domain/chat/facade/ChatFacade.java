package com.sofa.linkiving.domain.chat.facade;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.chat.ai.TitleClient;
import com.sofa.linkiving.domain.chat.dto.internal.MessagesDto;
import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.chat.dto.response.CreateChatRes;
import com.sofa.linkiving.domain.chat.dto.response.MessagesRes;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.service.ChatService;
import com.sofa.linkiving.domain.chat.service.FeedbackService;
import com.sofa.linkiving.domain.chat.service.MessageService;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatFacade {
	private final ChatService chatService;
	private final MessageService messageService;
	private final FeedbackService feedbackService;
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
		Chat chat = chatService.getChat(chatId, member);
		messageService.generateAnswer(chat, message);
	}

	public void cancelAnswer(Long chatId, Member member) {
		Chat chat = chatService.getChat(chatId, member);
		messageService.cancelAnswer(chat);
	}
}
