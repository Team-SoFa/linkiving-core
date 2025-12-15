package com.sofa.linkiving.domain.chat.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.ai.AiTitleClient;
import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.chat.dto.response.CreateChatRes;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.service.ChatService;
import com.sofa.linkiving.domain.chat.service.FeedbackService;
import com.sofa.linkiving.domain.chat.service.MessageService;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
public class ChatFacadeTest {
	@InjectMocks
	private ChatFacade chatFacade;

	@Mock
	private ChatService chatService;

	@Mock
	private MessageService messageService;

	@Mock
	private FeedbackService feedbackService;

	@Mock
	private AiTitleClient aiTitleClient;

	@Mock
	private Member member;

	@Test
	@DisplayName("ChatService.getChats 호출 및 ChatsRes 변환 반환")
	void shouldReturnChatsResWhenGetChats() {
		// given
		Chat chat = mock(Chat.class);
		given(chat.getId()).willReturn(1L);
		given(chat.getTitle()).willReturn("Title");

		given(chatService.getChats(member)).willReturn(List.of(chat));

		// when
		ChatsRes result = chatFacade.getChats(member);

		// then
		assertThat(result.chats()).hasSize(1);
		assertThat(result.chats().get(0).title()).isEqualTo("Title");

		verify(chatService).getChats(member);
	}

	@Test
	@DisplayName("createChat 호출 및 CreateChatRes 반환")
	void shouldReturnCreateChatResWhenCreateChat() {
		// given
		String firstChat = "안녕하세요";
		String generatedTitle = "요약된 제목";
		Long chatId = 100L;

		Chat mockChat = mock(Chat.class);

		given(mockChat.getId()).willReturn(chatId);
		given(mockChat.getTitle()).willReturn(generatedTitle);

		given(aiTitleClient.generateSummary(firstChat)).willReturn(generatedTitle);
		given(chatService.createChat(generatedTitle, member)).willReturn(mockChat);

		// when
		CreateChatRes result = chatFacade.createChat(firstChat, member);

		// then
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(chatId);
		assertThat(result.title()).isEqualTo(generatedTitle);
		assertThat(result.firstChat()).isEqualTo(firstChat);

		verify(chatService).createChat(generatedTitle, member);
	}

	@Test
	@DisplayName("채팅방 삭제 요청 시 하위 데이터(피드백, 메시지) 일괄 삭제 및 채팅방 제거 위임")
	void shouldDeleteAllRelatedDataWhenDeleteChat() {
		// given
		Long chatId = 1L;
		Chat chat = mock(Chat.class);

		given(chatService.getChat(chatId, member)).willReturn(chat);

		// when
		chatFacade.deleteChat(member, chatId);

		// then
		// 1. 피드백 삭제 호출 확인
		verify(feedbackService).deleteAll(chat);
		// 2. 메시지 삭제 호출 확인
		verify(messageService).deleteAll(chat);
		// 3. 채팅방 삭제 호출 확인
		verify(chatService).delete(chat);
	}
}
