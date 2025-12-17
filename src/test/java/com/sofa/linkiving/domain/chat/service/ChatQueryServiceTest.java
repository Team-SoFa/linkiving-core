package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.error.ChatErrorCode;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
public class ChatQueryServiceTest {

	@InjectMocks
	private ChatQueryService chatQueryService;

	@Mock
	private ChatRepository chatRepository;

	@Mock
	private Member member;

	@Test
	@DisplayName("ChatRepository.findByIdAndMember 호출 및 반환")
	void shouldReturnChatWhenFindChat() {
		// given
		Long chatId = 1L;
		Chat chat = mock(Chat.class);
		given(chatRepository.findByIdAndMember(chatId, member)).willReturn(Optional.of(chat));

		// when
		Chat result = chatQueryService.findChat(chatId, member);

		// then
		assertThat(result).isEqualTo(chat);
	}

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

	@Test
	@DisplayName("채팅방 조회 성공 시 Chat 엔티티 반환")
	void shouldReturnChatWhenChatExists() {
		// given
		Long chatId = 1L;
		Chat chat = mock(Chat.class);
		given(chatRepository.findByIdAndMember(chatId, member)).willReturn(Optional.of(chat));

		// when
		Chat result = chatQueryService.findChat(chatId, member);

		// then
		assertThat(result).isEqualTo(chat);
		verify(chatRepository).findByIdAndMember(chatId, member);
	}

	@Test
	@DisplayName("채팅방 미존재 시 BusinessException(CHAT_NOT_FOUND) 발생")
	void shouldThrowExceptionWhenChatNotFound() {
		// given
		Long chatId = 999L;
		given(chatRepository.findByIdAndMember(chatId, member)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> chatQueryService.findChat(chatId, member))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_NOT_FOUND);
	}
}
