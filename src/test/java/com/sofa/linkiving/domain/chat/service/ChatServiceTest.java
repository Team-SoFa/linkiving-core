package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

	@InjectMocks
	private ChatService chatService;

	@Mock
	private ChatCommandService chatCommandService;

	@Mock
	private ChatQueryService chatQueryService;

	@Mock
	private Member member;

	@Test
	@DisplayName("채팅방 목록 조회 시 ChatQueryService.findAll을 호출하고 결과 반환")
	void shouldReturnChatsWhenGetChats() {
		// given
		List<Chat> expectedChats = List.of(mock(Chat.class));
		given(chatQueryService.findAllOrderByLastMessageDesc(member)).willReturn(expectedChats);

		// when
		List<Chat> result = chatService.getChats(member);

		// then
		assertThat(result).isEqualTo(expectedChats);
		verify(chatQueryService).findAllOrderByLastMessageDesc(member);
	}

	@Test
	@DisplayName("createChat 요청 시 ChatCommandService.saveChat 위임")
	void shouldCallSaveChatWhenCreateChat() {
		// given
		String firstChat = "첫 대화입니다";
		Chat chat = mock(Chat.class);

		given(chatCommandService.saveChat(firstChat, member)).willReturn(chat);

		// when
		Chat result = chatService.createChat(firstChat, member);

		// then
		assertThat(result).isEqualTo(chat);
		verify(chatCommandService).saveChat(firstChat, member);
	}

	@Test
	@DisplayName("ChatQueryService.findChat 호출 및 결과 반환")
	void shouldReturnChatWhenGetChat() {
		// given
		Long chatId = 1L;
		Chat chat = mock(Chat.class);
		given(chatQueryService.findChat(chatId, member)).willReturn(chat);

		// when
		Chat result = chatService.getChat(chatId, member);

		// then
		assertThat(result).isEqualTo(chat);
		verify(chatQueryService).findChat(chatId, member);
	}

	@Test
	@DisplayName("ChatCommandService.deleteChat 호출 위임")
	void shouldCallDeleteChatWhenDelete() {
		// given
		Chat chat = mock(Chat.class);

		// when
		chatService.delete(chat);

		// then
		verify(chatCommandService).deleteChat(chat);
	}
}
