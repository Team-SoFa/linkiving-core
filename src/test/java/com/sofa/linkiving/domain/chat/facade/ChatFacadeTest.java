package com.sofa.linkiving.domain.chat.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.sofa.linkiving.domain.chat.ai.TitleClient;
import com.sofa.linkiving.domain.chat.dto.internal.MessagesDto;
import com.sofa.linkiving.domain.chat.dto.response.AnswerRes;
import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.chat.dto.response.CreateChatRes;
import com.sofa.linkiving.domain.chat.dto.response.MessagesRes;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.manager.TaskManager;
import com.sofa.linkiving.domain.chat.service.ChatService;
import com.sofa.linkiving.domain.chat.service.FeedbackService;
import com.sofa.linkiving.domain.chat.service.MessageService;
import com.sofa.linkiving.domain.chat.service.RagChatService;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
public class ChatFacadeTest {
	@InjectMocks
	private ChatFacade chatFacade;

	@Mock
	private ChatService chatService;

	@Mock
	private MessageService messageService;

	@Mock
	private FeedbackService feedbackService;

	@Mock
	private TitleClient titleClient;

	@Mock
	private RagChatService ragChatService;

	@Mock
	private TaskManager taskManager;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private Member member;

	@Test
	@DisplayName("특정 채팅방의 메시지 목록을 조회한다")
	void shouldGetMessages() {
		// given
		Long chatId = 1L;
		Long lastId = 100L;
		int size = 20;

		Member member = mock(Member.class);
		Chat chat = mock(Chat.class);

		given(chatService.getChat(chatId, member)).willReturn(chat);

		MessagesDto messagesDto = new MessagesDto(Collections.emptyList(), false);
		given(messageService.getMessages(chat, lastId, size)).willReturn(messagesDto);

		// when
		MessagesRes result = chatFacade.getMessages(member, chatId, lastId, size);

		// then
		assertThat(result).isNotNull();
		assertThat(result.messages()).isEmpty();
		assertThat(result.hasNext()).isFalse();

		verify(chatService).getChat(chatId, member);
		verify(messageService).getMessages(chat, lastId, size);
	}

	@Test
	@DisplayName("ChatService.getChats 호출 및 ChatsRes 변환 반환")
	void shouldReturnChatsResWhenGetChats() {
		// given
		Chat chat = mock(Chat.class);
		given(chat.getId()).willReturn(1L);
		given(chat.getTitle()).willReturn("Title");

		given(chatService.getChats(member)).willReturn(List.of(chat));

		// when
		ChatsRes result = chatFacade.getChats(member);

		// then
		assertThat(result.chats()).hasSize(1);
		assertThat(result.chats().get(0).title()).isEqualTo("Title");

		verify(chatService).getChats(member);
	}

	@Test
	@DisplayName("createChat 호출 및 CreateChatRes 반환")
	void shouldReturnCreateChatResWhenCreateChat() {
		// given
		String firstChat = "안녕하세요";
		String generatedTitle = "요약된 제목";
		Long chatId = 100L;

		Chat mockChat = mock(Chat.class);

		given(mockChat.getId()).willReturn(chatId);
		given(mockChat.getTitle()).willReturn(generatedTitle);

		given(titleClient.generateTitle(firstChat)).willReturn(generatedTitle);
		given(chatService.createChat(generatedTitle, member)).willReturn(mockChat);

		// when
		CreateChatRes result = chatFacade.createChat(firstChat, member);

		// then
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(chatId);
		assertThat(result.title()).isEqualTo(generatedTitle);
		assertThat(result.firstChat()).isEqualTo(firstChat);

		verify(chatService).createChat(generatedTitle, member);
	}

	@Test
	@DisplayName("채팅방 삭제 요청 시 하위 데이터(피드백, 메시지) 일괄 삭제 및 채팅방 제거 위임")
	void shouldDeleteAllRelatedDataWhenDeleteChat() {
		// given
		Long chatId = 1L;
		Chat chat = mock(Chat.class);

		given(chatService.getChat(chatId, member)).willReturn(chat);

		// when
		chatFacade.deleteChat(member, chatId);

		// then
		// 1. 피드백 삭제 호출 확인
		verify(feedbackService).deleteAll(chat);
		// 2. 메시지 삭제 호출 확인
		verify(messageService).deleteAll(chat);
		// 3. 채팅방 삭제 호출 확인
		verify(chatService).delete(chat);
	}

	@Test
	@DisplayName("답변 생성이 성공하면 TaskManager에서 제거하고 성공 알림 전송")
	void shouldSendNotificationWhenAnswerGeneratedSuccessfully() {
		// given
		Long chatId = 1L;
		String userMessage = "질문입니다";
		member = mock(Member.class);
		given(member.getEmail()).willReturn("test@test.com");

		CompletableFuture<AnswerRes> future = new CompletableFuture<>();

		given(ragChatService.generateAnswer(chatId, member, userMessage)).willReturn(future);

		// when
		chatFacade.generateAnswer(chatId, member, userMessage);

		// then
		verify(taskManager).put(chatId, future);

		AnswerRes successRes = mock(AnswerRes.class);
		future.complete(successRes);

		verify(taskManager).remove(chatId);

		verify(messagingTemplate).convertAndSendToUser(
			eq(member.getEmail()),
			eq("/queue/chat"),
			eq(successRes)
		);
	}

	@Test
	@DisplayName("답변 생성 중 예외가 발생하면 에러 알림 전송")
	void shouldSendErrorNotificationWhenExceptionOccurs() {
		// given
		Long chatId = 1L;
		String userMessage = "질문입니다";
		member = mock(Member.class);
		given(member.getEmail()).willReturn("test@test.com");

		CompletableFuture<AnswerRes> future = new CompletableFuture<>();
		given(ragChatService.generateAnswer(chatId, member, userMessage)).willReturn(future);

		// when
		chatFacade.generateAnswer(chatId, member, userMessage);

		// then
		verify(taskManager).put(chatId, future);

		// 2. 비동기 작업 완료 시뮬레이션 (예외 발생)
		future.completeExceptionally(new RuntimeException("AI Server Error"));

		// 3. 콜백 실행 후 TaskManager 제거 및 에러 전송 확인
		verify(taskManager).remove(chatId);

		// 에러 발생 시 AnswerRes.error(...)가 전송되어야 함
		verify(messagingTemplate).convertAndSendToUser(
			eq(member.getEmail()),
			eq("/queue/chat"),
			any(AnswerRes.class) // AnswerRes.error() 결과
		);
	}

	@Test
	@DisplayName("작업이 취소되면 에러 알림 전송")
	void shouldSendErrorNotificationWhenTaskIsCancelled() {
		// given
		Long chatId = 1L;
		String userMessage = "질문입니다";
		member = mock(Member.class);
		given(member.getEmail()).willReturn("test@test.com");

		CompletableFuture<AnswerRes> future = new CompletableFuture<>();
		given(ragChatService.generateAnswer(chatId, member, userMessage)).willReturn(future);

		// when
		chatFacade.generateAnswer(chatId, member, userMessage);

		// 2. 작업 취소 시뮬레이션
		future.cancel(true);

		// then
		verify(taskManager).remove(chatId);

		// 취소 상태일 때도 에러 메시지 전송 로직을 타는지 확인
		verify(messagingTemplate).convertAndSendToUser(
			eq(member.getEmail()),
			eq("/queue/chat"),
			any(AnswerRes.class)
		);
	}

	@Test
	@DisplayName("존재하는 채팅방인 경우 TaskManager에 취소 요청")
	void shouldCancelTaskWhenChatExists() {
		// given
		Long chatId = 1L;
		member = mock(Member.class);

		given(chatService.existsChat(member, chatId)).willReturn(true);

		// when
		chatFacade.cancelAnswer(chatId, member);

		// then
		verify(taskManager).cancel(chatId);
	}

	@Test
	@DisplayName("존재하지 않는 채팅방인 경우 아무 작업도 하지 않음")
	void shouldNotCancelTaskWhenChatDoesNotExist() {
		// given
		Long chatId = 1L;
		member = mock(Member.class);

		given(chatService.existsChat(member, chatId)).willReturn(false);

		// when
		chatFacade.cancelAnswer(chatId, member);

		// then
		verify(taskManager, never()).cancel(anyLong());
	}
}
