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
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;

@ExtendWith(MockitoExtension.class)
public class MessageQueryServiceTest {

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
}
