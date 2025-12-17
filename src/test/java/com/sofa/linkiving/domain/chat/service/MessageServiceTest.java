package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.chat.dto.internal.MessagesDto;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.manager.SubscriptionManager;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.service.SummaryQueryService;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

	@InjectMocks
	private MessageService messageService;

	@Mock
	private MessageCommandService messageCommandService;

	@Mock
	private MessageQueryService messageQueryService;

	@Mock
	private SummaryQueryService summaryQueryService;

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
	@DisplayName("메시지 목록과 포함된 링크의 요약을 정상적으로 조회하여 DTO로 반환한다")
	void shouldGetMessagesWithLinksAndSummaries() {
		// given
		Chat chat = mock(Chat.class);
		Long lastId = 100L;
		int size = 10;

		Link link1 = mock(Link.class);
		given(link1.getId()).willReturn(1L);
		Summary summary1 = mock(Summary.class);

		Link link2 = mock(Link.class);
		given(link2.getId()).willReturn(2L);

		Message msg1 = mock(Message.class);
		given(msg1.getLinks()).willReturn(List.of(link1));

		Message msg2 = mock(Message.class);
		given(msg2.getLinks()).willReturn(List.of(link2));

		List<Message> messages = List.of(msg1, msg2);
		Slice<Message> messageSlice = new SliceImpl<>(messages);

		given(messageQueryService.findAllByChatAndCursor(chat, lastId, size))
			.willReturn(messageSlice);

		given(summaryQueryService.getSelectedSummariesByLinks(anyList()))
			.willReturn(Map.of(1L, summary1));

		// when
		MessagesDto result = messageService.getMessages(chat, lastId, size);

		// then
		assertThat(result).isNotNull();
		assertThat(result.messageDtos()).hasSize(2);
		assertThat(result.hasNext()).isFalse();

		var msgDto1 = result.messageDtos().get(0);
		assertThat(msgDto1.linkDtos()).hasSize(1);
		assertThat(msgDto1.linkDtos().get(0).summary()).isEqualTo(summary1);

		var msgDto2 = result.messageDtos().get(1);
		assertThat(msgDto2.linkDtos()).hasSize(1);
		assertThat(msgDto2.linkDtos().get(0).summary()).isNull(); // Map에 없으므로 null

		// Verify
		verify(messageQueryService, times(1)).findAllByChatAndCursor(chat, lastId, size);
		verify(summaryQueryService, times(1)).getSelectedSummariesByLinks(anyList());
	}

	@Test
	@DisplayName("메시지가 없을 경우 빈 목록을 반환한다")
	void shouldReturnEmptyWhenNoMessages() {
		// given
		Chat chat = mock(Chat.class);
		Long lastId = null;
		int size = 10;

		Slice<Message> emptySlice = new SliceImpl<>(Collections.emptyList());

		given(messageQueryService.findAllByChatAndCursor(chat, lastId, size))
			.willReturn(emptySlice);

		// 빈 리스트가 넘어가면 SummaryService는 호출되지만 빈 맵을 반환하도록 설정 (혹은 실제 로직에 따라 호출됨)
		given(summaryQueryService.getSelectedSummariesByLinks(anyList()))
			.willReturn(Collections.emptyMap());

		// when
		MessagesDto result = messageService.getMessages(chat, lastId, size);

		// then
		assertThat(result.messageDtos()).isEmpty();
		assertThat(result.hasNext()).isFalse();
	}

	@Test
	@DisplayName("중복된 링크가 있어도 요약 조회 시에는 중복을 제거하여 요청한다")
	void shouldRequestSummariesForDistinctLinks() {
		// given
		Chat chat = mock(Chat.class);

		Link link1 = mock(Link.class); // 동일한 객체

		Message msg1 = mock(Message.class);
		given(msg1.getLinks()).willReturn(List.of(link1));

		Message msg2 = mock(Message.class);
		given(msg2.getLinks()).willReturn(List.of(link1)); // msg1과 같은 링크 포함

		Slice<Message> messageSlice = new SliceImpl<>(List.of(msg1, msg2));

		given(messageQueryService.findAllByChatAndCursor(any(), any(), anyInt()))
			.willReturn(messageSlice);
		given(summaryQueryService.getSelectedSummariesByLinks(anyList()))
			.willReturn(Collections.emptyMap());

		// when
		messageService.getMessages(chat, null, 10);

		// then
		verify(summaryQueryService).getSelectedSummariesByLinks(argThat(list ->
			list.size() == 1 // 두 메시지에 링크가 총 2개지만, 같은 객체이므로 1개로 줄어야 함
		));
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
