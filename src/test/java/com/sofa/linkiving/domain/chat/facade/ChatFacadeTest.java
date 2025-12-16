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

import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
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
}
