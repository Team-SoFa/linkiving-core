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
	private ChatQueryService chatQueryService;

	@Mock
	private ChatCommandService chatCommandService;

	@Mock
	private Member member;

	@Test
	@DisplayName("채팅방 목록 조회 시 ChatQueryService.findAll을 호출하고 결과 반환")
	void shouldReturnChatsWhenGetChats() {
		// given
		List<Chat> expectedChats = List.of(mock(Chat.class));
		given(chatQueryService.findAll(member)).willReturn(expectedChats);

		// when
		List<Chat> result = chatService.getChats(member);

		// then
		assertThat(result).isEqualTo(expectedChats);
		verify(chatQueryService).findAll(member);
	}
}
