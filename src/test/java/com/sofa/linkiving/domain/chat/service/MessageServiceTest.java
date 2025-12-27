package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.chat.dto.internal.MessagesDto;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.manager.SubscriptionManager;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

	@InjectMocks
	private MessageService messageService;

	@Mock
	private MessageCommandService messageCommandService;

	@Mock
	private MessageQueryService messageQueryService;

	@Mock
	private Chat chat;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private SubscriptionManager subscriptionManager;

	@BeforeEach
	void setUp() {
		lenient().when(chat.getId()).thenReturn(1L);
	}

	@Test
	@DisplayName("MessageCommandService.deleteAllByChat 호출 위임")
	void shouldCallDeleteAllByChatWhenDeleteAll() {
		// given
		Chat chat = mock(Chat.class);

		// when
		messageService.deleteAll(chat);

		// then
		verify(messageCommandService).deleteAllByChat(chat);
	}

	@Test
	@DisplayName("메시지 조회 요청 시 QueryService를 호출하여 결과를 반환함")
	void shouldDelegateToQueryServiceWhenGetMessages() {
		// given
		Long lastId = 1L;
		int size = 20;
		MessagesDto expectedDto = new MessagesDto(Collections.emptyList(), false);

		given(messageQueryService.findAllByChatAndCursor(chat, lastId, size)).willReturn(expectedDto);

		// when
		MessagesDto result = messageService.getMessages(chat, lastId, size);

		// then
		assertThat(result).isEqualTo(expectedDto);
		verify(messageQueryService).findAllByChatAndCursor(chat, lastId, size);
	}

	@Test
	@DisplayName("답변 취소 요청 시 구독 취소 및 취소 메시지 전송")
	void shouldCancelSubscriptionAndSendMessageWhenCancelAnswer() {
		// given
		String roomId = "1";

		// when
		messageService.cancelAnswer(chat);

		// then
		verify(subscriptionManager).cancel(roomId);
		verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + roomId), eq("GENERATION_CANCELLED"));
	}

	@Test
	@DisplayName("이미 답변 생성 중일 경우 중복 요청 무시")
	void shouldIgnoreRequestWhenAlreadyGenerating() {
		// given
		// messageBuffers 필드에 강제로 현재 채팅방 ID를 넣어 생성 중인 상태로 만듦
		@SuppressWarnings("unchecked")
		Map<String, StringBuilder> buffers = (Map<String, StringBuilder>)ReflectionTestUtils.getField(messageService,
			"messageBuffers");
		Assertions.assertNotNull(buffers);
		buffers.put("1", new StringBuilder());

		// when
		messageService.generateAnswer(chat, "질문");

		// then
		// WebClient 호출 로직으로 넘어가지 않아야 하므로 SubscriptionManager 호출이 없어야 함
		verify(subscriptionManager, never()).add(anyString(), any());
	}
}
