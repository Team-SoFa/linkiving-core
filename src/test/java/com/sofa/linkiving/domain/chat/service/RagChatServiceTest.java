package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.ai.AnswerClient;
import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.AnswerRes;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.service.LinkQueryService;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagChatService 단위 테스트")
public class RagChatServiceTest {

	@InjectMocks
	private RagChatService ragChatService;

	@Mock
	private AnswerClient answerClient;

	@Mock
	private MessageCommandService messageCommandService;

	@Mock
	private MessageQueryService messageQueryService;

	@Mock
	private LinkQueryService linkQueryService;

	@Mock
	private ChatQueryService chatQueryService;

	private Member member;
	private Chat chat;
	private final Long chatId = 1L;
	private final String userMessage = "테스트 질문";

	@BeforeEach
	void setUp() {
		member = mock(Member.class);
		lenient().when(member.getId()).thenReturn(100L);

		chat = mock(Chat.class);
		lenient().when(chat.getId()).thenReturn(chatId);
	}

	@Test
	@DisplayName(" 정상 흐름일 때 AI 응답을 처리하고 결과를 반환한다")
	void shouldReturnAnswerRes_WhenProcessSuccessfully() throws ExecutionException, InterruptedException {
		// given
		// 1. Chat 조회
		given(chatQueryService.findChat(chatId, member)).willReturn(chat);

		// 2. 유저 메시지 저장
		Message questionMsg = mock(Message.class);
		given(questionMsg.getId()).willReturn(50L);
		given(messageCommandService.saveUserMessage(chat, userMessage)).willReturn(questionMsg);

		// 3. 과거 대화 내역 조회
		Message historyMsg = mock(Message.class);
		given(historyMsg.getContent()).willReturn("이전 대화");
		given(messageQueryService.findTop7ByChatIdAndIdLessThanOrderByIdDesc(50L, chat))
			.willReturn(List.of(historyMsg));

		// 4. AI Client 응답 설정 (유효한 링크 ID와 무효한 ID 혼합)
		RagAnswerRes ragRes = new RagAnswerRes(
			"AI 답변입니다.",
			List.of("10", " invalid ", " 20 "),
			List.of(new RagAnswerRes.ReasoningStep("생각 과정", List.of("10"))),
			List.of("10", "20"),
			false
		);
		given(answerClient.generateAnswer(any(RagAnswerReq.class))).willReturn(ragRes);

		// 5. 링크 조회
		LinkDto linkDto1 = mock(LinkDto.class);
		Link link1 = mock(Link.class);
		given(linkDto1.link()).willReturn(link1);

		given(linkQueryService.findAllByIdInWithSummary(eq(List.of(10L, 20L)), eq(member)))
			.willReturn(List.of(linkDto1));

		// 6. AI 메시지 저장
		Message answerMsg = mock(Message.class);
		given(answerMsg.getId()).willReturn(51L);
		given(answerMsg.getContent()).willReturn("AI 답변입니다.");
		given(messageCommandService.saveAiMessage(eq(chat), anyString(), anyList()))
			.willReturn(answerMsg);

		// when
		CompletableFuture<AnswerRes> future = ragChatService.generateAnswer(chatId, member, userMessage);

		// then
		AnswerRes result = future.get();

		assertThat(result).isNotNull();
		assertThat(result.chatId()).isEqualTo(chatId);
		assertThat(result.content()).isEqualTo("AI 답변입니다.");
		assertThat(result.links()).hasSize(1);

		// 순서대로 호출되었는지 검증
		verify(messageCommandService).saveUserMessage(chat, userMessage);
		verify(answerClient).generateAnswer(any(RagAnswerReq.class));
		verify(linkQueryService).findAllByIdInWithSummary(eq(List.of(10L, 20L)), eq(member));
		verify(messageCommandService).saveAiMessage(chat, "AI 답변입니다.", List.of(link1));
	}

	@Test
	@DisplayName("채팅방이 존재하지 않으면 예외 발생")
	void shouldThrowException_WhenChatNotFound() {
		// given
		given(chatQueryService.findChat(chatId, member))
			.willThrow(new RuntimeException("Chat Not Found"));

		// when & then
		assertThatThrownBy(() -> ragChatService.generateAnswer(chatId, member, userMessage))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Chat Not Found");

		verifyNoInteractions(answerClient);
	}

	@Test
	@DisplayName("AI 클라이언트 오류 발생 시 예외 전파")
	void shouldThrowException_WhenAiClientFails() {
		// given
		given(chatQueryService.findChat(chatId, member)).willReturn(chat);

		Message questionMsg = mock(Message.class);
		given(questionMsg.getId()).willReturn(50L);
		given(messageCommandService.saveUserMessage(chat, userMessage)).willReturn(questionMsg);

		given(messageQueryService.findTop7ByChatIdAndIdLessThanOrderByIdDesc(anyLong(), any()))
			.willReturn(Collections.emptyList());

		given(answerClient.generateAnswer(any()))
			.willThrow(new RuntimeException("AI Service Unavailable"));

		// when & then
		assertThatThrownBy(() -> ragChatService.generateAnswer(chatId, member, userMessage))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("AI Service Unavailable");
	}
}
