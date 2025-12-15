package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
public class ChatCommandServiceTest {

	@InjectMocks
	private ChatCommandService chatCommandService;

	@Mock
	private ChatRepository chatRepository;

	@Mock
	private Member member;

	@Test
	@DisplayName("ChatRepository.save 호출 및 저장된 Chat 반환")
	void shouldReturnSavedChatWhenSaveChat() {
		// given
		String firstChat = "AI 관련 자료 찾아줘";
		Chat chat = Chat.builder()
			.title(firstChat)
			.member(member)
			.build();

		given(chatRepository.save(any(Chat.class))).willReturn(chat);

		// when
		Chat result = chatCommandService.saveChat(firstChat, member);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo(firstChat);

		verify(chatRepository).save(any(Chat.class));
	}

	@Test
	@DisplayName("ChatRepository.delete 호출")
	void shouldCallDeleteWhenDeleteChat() {
		// given
		Chat chat = mock(Chat.class);

		// when
		chatCommandService.deleteChat(chat);

		// then
		verify(chatRepository).delete(chat);
	}
}
