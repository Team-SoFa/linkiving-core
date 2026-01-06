package com.sofa.linkiving.domain.chat.integration;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sofa.linkiving.domain.chat.ai.AnswerClient;
import com.sofa.linkiving.domain.chat.dto.request.AnswerCancelReq;
import com.sofa.linkiving.domain.chat.dto.request.AnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.AnswerRes;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class WebSocketChatIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private SummaryRepository summaryRepository;

	@MockitoBean
	private RedisService redisService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AnswerClient answerClient;

	private StompSession stompSession;
	private BlockingQueue<AnswerRes> blockingQueue;
	private Chat testChat;

	@BeforeEach
	void setUp() throws ExecutionException, InterruptedException, TimeoutException {
		messageRepository.deleteAllInBatch();
		chatRepository.deleteAllInBatch();
		summaryRepository.deleteAllInBatch();
		linkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();

		// 1. 데이터 초기화
		Member testMember = memberRepository.save(Member.builder()
			.email("test@test.com")
			.password("password")
			.build());

		testChat = chatRepository.save(Chat.builder()
			.member(testMember)
			.title("테스트 채팅방")
			.build());

		RagAnswerRes defaultRes = new RagAnswerRes(
			"Gemini와 관련된 내용은 두 개의 아티클에 포함돼 있습니다.",
			List.of("3", "4"),
			List.of(new RagAnswerRes.ReasoningStep("제공된 컨텍스트 중...", List.of("3", "4"))),
			List.of("3", "4"),
			false
		);
		given(answerClient.generateAnswer(any())).willReturn(defaultRes);

		// 2. STOMP 클라이언트 설정
		WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));

		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setObjectMapper(new ObjectMapper().registerModule(new JavaTimeModule()));
		stompClient.setMessageConverter(converter);

		this.blockingQueue = new LinkedBlockingQueue<>();

		// 3. WebSocket 연결
		String wsUrl = "ws://localhost:" + port + "/ws/chat";
		StompHeaders headers = new StompHeaders();

		String validToken = jwtTokenProvider.createAccessToken(testMember.getEmail());
		headers.add("Authorization", "Bearer " + validToken);

		this.stompSession = stompClient.connectAsync(
			wsUrl,
			new WebSocketHttpHeaders(),
			headers,
			new StompSessionHandlerAdapter() {
			}
		).get(5, SECONDS);
	}

	@AfterEach
	void tearDown() {
		messageRepository.deleteAllInBatch();
		chatRepository.deleteAllInBatch();
		summaryRepository.deleteAllInBatch();
		linkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	private List<Transport> createTransportClient() {
		List<Transport> transports = new ArrayList<>();
		transports.add(new WebSocketTransport(new StandardWebSocketClient()));
		return transports;
	}

	private void subscribeToChatQueue() {
		stompSession.subscribe("/user/queue/chat", new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return AnswerRes.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				blockingQueue.offer((AnswerRes)payload);
			}
		});
	}

	@Test
	@DisplayName("유저가 메시지를 보내면 AI 응답이 Queue로 수신된다")
	void shouldReceiveAnswerWhenMessageSent() throws InterruptedException {
		// given
		Long chatId = testChat.getId();
		String userMessage = "Gemini에 대해 알려줘";
		AnswerReq req = new AnswerReq(chatId, userMessage);

		subscribeToChatQueue();

		// when
		stompSession.send("/ws/chat/send", req);

		// then
		AnswerRes received = blockingQueue.poll(10, SECONDS);

		assertThat(received).isNotNull();
		assertThat(received.chatId()).isEqualTo(chatId);
		assertThat(received.success()).isTrue();
		assertThat(received.content()).contains("Gemini와 관련된 내용");
	}

	@Test
	@DisplayName("답변 생성 중 취소 요청 시, 작업이 중단되고 실패 메시지가 수신된다")
	void shouldReceiveErrorMessageWhenCancelled() throws InterruptedException {
		// given
		Long chatId = testChat.getId();
		String userMessage = "취소될 질문";
		AnswerReq sendReq = new AnswerReq(chatId, userMessage);
		AnswerCancelReq cancelReq = new AnswerCancelReq(chatId);

		given(answerClient.generateAnswer(any())).willAnswer(invocation -> {
			Thread.sleep(500);
			return new RagAnswerRes("지연된 답변", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				true);
		});

		subscribeToChatQueue();

		// when
		stompSession.send("/ws/chat/send", sendReq);
		Thread.sleep(50);
		stompSession.send("/ws/chat/cancel", cancelReq);

		// then
		AnswerRes received = blockingQueue.poll(5, SECONDS);

		assertThat(received).isNotNull();
		assertThat(received.chatId()).isEqualTo(chatId);
		assertThat(received.success()).isFalse();
		assertThat(received.content()).isEqualTo(userMessage);
	}

}
