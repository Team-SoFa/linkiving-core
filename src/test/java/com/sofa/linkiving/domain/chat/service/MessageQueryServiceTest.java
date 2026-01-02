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
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.error.MessageErrorCode;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
public class MessageQueryServiceTest {

	@Mock
	Member member;
	@InjectMocks
	private MessageQueryService messageQueryService;
	@Mock
	private MessageRepository messageRepository;

	@Test
	@DisplayName("MessageRepository.findAllByChat 호출 및 결과 반환")
	void shouldReturnMessagesWhenFindAllByChat() {
		// given
		Chat chat = mock(Chat.class);
		List<Message> expectedMessages = List.of(mock(Message.class));

		given(messageRepository.findAllByChat(chat)).willReturn(expectedMessages);

		// when
		List<Message> result = messageQueryService.findAllByChat(chat);

		// then
		assertThat(result).isEqualTo(expectedMessages);
		verify(messageRepository).findAllByChat(chat);
	}

	@Test
	@DisplayName("단일 메시지 조회 시 메시지가 존재하고 회원이 일치하면 메시지를 반환함")
	void shouldReturnMessageWhenFound() {
		// given
		Long messageId = 1L;
		Message message = mock(Message.class);

		given(messageRepository.findByIdAndMember(messageId, member)).willReturn(Optional.of(message));

		// when
		Message result = messageQueryService.findByIdAndMember(messageId, member);

		// then
		assertThat(result).isEqualTo(message);
	}

	@Test
	@DisplayName("단일 메시지 조회 시 메시지가 없거나 회원이 불일치하면 예외를 던짐")
	void shouldThrowExceptionWhenNotFound() {
		// given
		Long messageId = 999L;

		given(messageRepository.findByIdAndMember(messageId, member)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> messageQueryService.findByIdAndMember(messageId, member))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MessageErrorCode.MESSAGE_NOT_FOUND);
	}
}

