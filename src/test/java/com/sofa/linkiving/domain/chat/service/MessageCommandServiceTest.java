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
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;

@ExtendWith(MockitoExtension.class)
public class MessageCommandServiceTest {

	@InjectMocks
	private MessageCommandService messageCommandService;

	@Mock
	private MessageRepository messageRepository;

	@Test
	@DisplayName("MessageRepository.save 호출 및 저장된 Message 반환")
	void shouldReturnSavedMessageWhenSaveMessage() {
		// given
		Message message = mock(Message.class);
		given(messageRepository.save(any(Message.class))).willReturn(message);

		// when
		Message result = messageCommandService.saveMessage(message);

		// then
		assertThat(result).isEqualTo(message);
		verify(messageRepository).save(message);
	}

	@Test
	@DisplayName("MessageRepository.deleteAllByChat 호출")
	void shouldCallDeleteAllByChatWhenDeleteAllByChat() {
		// given
		Chat chat = mock(Chat.class);

		// when
		messageCommandService.deleteAllByChat(chat);

		// then
		verify(messageRepository).deleteAllByChat(chat);
	}
}
