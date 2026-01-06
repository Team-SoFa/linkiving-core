package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

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

	@Mock
	private Chat chat;

	@Test
	@DisplayName("요청 개수 초과 데이터가 존재 시 hasNext=true 반환 및 데이터를 잘라서 반환: (요청 개수 :10개 ,데이터 :11개)")
	void shouldReturnHasNextTrueWhenMoreDataExists() {
		// given
		Long lastId = 100L;
		int size = 10;

		List<Message> messageDtos = new ArrayList<>();
		for (int i = 0; i < size + 1; i++) {
			messageDtos.add(mock(Message.class));
		}

		given(messageRepository.findAllByChatAndCursor(eq(chat), eq(lastId), any(Pageable.class)))
			.willReturn(messageDtos);

		// when
		Slice<Message> result = messageQueryService.findAllByChatAndCursor(chat, lastId, size);

		// then
		assertThat(result.hasNext()).isTrue();
		assertThat(result.getContent()).hasSize(size);
	}

	@Test
	@DisplayName("요청 개수 이하로 데이터 존재 시 hasNext=false 반환 (요청 개수 :10개 ,데이터 :10개)")
	void shouldReturnHasNextFalseWhenNoMoreData() {
		// given
		Long lastId = 100L;
		int size = 10;

		List<Message> messageDtos = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			messageDtos.add(mock(Message.class));
		}

		given(messageRepository.findAllByChatAndCursor(eq(chat), eq(lastId), any(Pageable.class)))
			.willReturn(messageDtos);

		// when
		Slice<Message> result = messageQueryService.findAllByChatAndCursor(chat, lastId, size);

		// then
		assertThat(result.hasNext()).isFalse();
		assertThat(result.getContent()).hasSize(size);
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

