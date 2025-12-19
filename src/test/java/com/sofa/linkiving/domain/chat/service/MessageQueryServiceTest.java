package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.error.MessageErrorCode;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
public class MessageQueryServiceTest {
	@InjectMocks
	private MessageQueryService messageQueryService;

	@Mock
	private MessageRepository messageRepository;

	@Test
	@DisplayName("ID로 메시지 조회 시 존재하면 해당 엔티티를 반환함")
	void shouldReturnMessageWhenExists() {
		// given
		Long messageId = 1L;
		Message message = mock(Message.class);
		given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

		// when
		Message result = messageQueryService.findById(messageId);

		// then
		assertThat(result).isEqualTo(message);
	}

	@Test
	@DisplayName("ID로 메시지 조회 시 존재하지 않으면 CHAT_NOT_FOUND 예외가 발생함")
	void shouldThrowExceptionWhenNotFound() {
		// given
		Long messageId = 999L;
		given(messageRepository.findById(messageId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> messageQueryService.findById(messageId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", MessageErrorCode.CHAT_NOT_FOUND);
	}
}

