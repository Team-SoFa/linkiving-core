package com.sofa.linkiving.domain.chat.service;

import static org.mockito.Mockito.*;

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

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.manager.SubscriptionManager;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

	@InjectMocks
	private MessageService messageService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private SubscriptionManager subscriptionManager;

	@Mock
	private Chat chat;

	@BeforeEach
	void setUp() {
		// Chat ID Mocking
		lenient().when(chat.getId()).thenReturn(1L);
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
