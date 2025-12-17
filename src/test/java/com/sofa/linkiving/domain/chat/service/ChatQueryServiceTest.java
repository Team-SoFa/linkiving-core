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
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
public class ChatQueryServiceTest {
	@InjectMocks
	private ChatQueryService chatQueryService;

	@Mock
	private ChatRepository chatRepository;

	@Mock
	private Member member;

	@Test
	@DisplayName("ChatRepository.findAllByMemberOrderByCreatedAtDesc 호출 및 반환")
	void shouldReturnChatListWhenFindAllOrderByLastMessageDesc() {
		// given
		List<Chat> chats = List.of(mock(Chat.class));
		given(chatRepository.findAllByMemberOrderByLastMessageDesc(member)).willReturn(chats);

		// when
		List<Chat> result = chatQueryService.findAllOrderByLastMessageDesc(member);

		// then
		assertThat(result).isEqualTo(chats);
		verify(chatRepository).findAllByMemberOrderByLastMessageDesc(member);
	}
}
