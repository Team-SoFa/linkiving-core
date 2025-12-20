package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.sofa.linkiving.domain.chat.dto.internal.MessagesDto;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;

@ExtendWith(MockitoExtension.class)
public class MessageQueryServiceTest {

	@InjectMocks
	private MessageQueryService messageQueryService;

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private Chat chat;

	@Test
	@DisplayName("요청 개수 초과 데이터가 존재 시 hasNext=true 반환 및 데이터를 잘라서 반환: (요청 개수 :10개 ,데이터 :11개)")
	void shouldReturnHasNextTrueWhenMoreDataExists() {
		// given
		Long lastId = 100L;
		int size = 10;

		List<Message> messages = new ArrayList<>();
		for (int i = 0; i < size + 1; i++) {
			messages.add(mock(Message.class));
		}

		given(messageRepository.findAllByChatAndCursor(eq(chat), eq(lastId), any(Pageable.class)))
			.willReturn(messages);

		// when
		MessagesDto result = messageQueryService.findAllByChatAndCursor(chat, lastId, size);

		// then
		assertThat(result.hasNext()).isTrue();
		assertThat(result.messages()).hasSize(size);
	}

	@Test
	@DisplayName("요청 개수 이하로 데이터 존재 시 hasNext=false 반환 (요청 개수 :10개 ,데이터 :10개)")
	void shouldReturnHasNextFalseWhenNoMoreData() {
		// given
		Long lastId = 100L;
		int size = 10;

		List<Message> messages = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			messages.add(mock(Message.class));
		}

		given(messageRepository.findAllByChatAndCursor(eq(chat), eq(lastId), any(Pageable.class)))
			.willReturn(messages);

		// when
		MessagesDto result = messageQueryService.findAllByChatAndCursor(chat, lastId, size);

		// then
		assertThat(result.hasNext()).isFalse();
		assertThat(result.messages()).hasSize(size);
	}
}
