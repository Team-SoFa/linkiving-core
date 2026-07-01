package com.sofa.linkiving.domain.chat.integration;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.domain.chat.ai.AnswerClient;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.global.util.HashidsUtils;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;
import com.sofa.linkiving.security.jwt.error.CustomJwtException;
import com.sofa.linkiving.security.jwt.error.JwtErrorCode;

import jakarta.annotation.Nonnull;

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

	@MockitoSpyBean
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private HashidsUtils hashidsUtils;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AnswerClient answerClient;

	private StompSession stompSession;
	private BlockingQueue<Map<String, Object>> blockingQueue;
	private Chat testChat;
	private Member testMember;

	@BeforeEach
	void setUp() throws ExecutionException, InterruptedException, TimeoutException {
		messageRepository.deleteAllInBatch();
		chatRepository.deleteAllInBatch();
		summaryRepository.deleteAllInBatch();
		linkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();

		// 1. 데이터 초기화
		testMember = memberRepository.save(Member.builder()
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
		converter.setObjectMapper(objectMapper);
		stompClient.setMessageConverter(converter);

		this.blockingQueue = new LinkedBlockingQueue<>();

		// 3. WebSocket 연결
		String wsUrl = "ws://localhost:" + port + "/ws/chat";

		String validToken = jwtTokenProvider.createAccessToken(testMember.getEmail());
		String authHeaderValue = "Bearer " + validToken;

		StompHeaders headers = new StompHeaders();
		headers.add("Authorization", authHeaderValue);

		WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
		webSocketHttpHeaders.add(HttpHeaders.COOKIE, "accessToken=" + validToken);

		this.stompSession = stompClient.connectAsync(
			wsUrl,
			webSocketHttpHeaders,
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
			@Nonnull
			@Override
			public Type getPayloadType(@Nonnull StompHeaders headers) {
				return Map.class;
			}

			@Override
			public void handleFrame(@Nonnull StompHeaders headers, Object payload) {
				if (payload != null) {
					blockingQueue.add((Map<String, Object>)payload);
				}
			}
		});
	}

	@Test
	@DisplayName("쿠키에 담긴 accessToken으로 정상적으로 웹소켓 연결이 가능하다")
	void shouldConnectSuccessfullyWithCookie() throws Exception {
		// given
		WebSocketStompClient customClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
		String validToken = jwtTokenProvider.createAccessToken(testMember.getEmail());

		WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
		httpHeaders.add(HttpHeaders.COOKIE, "accessToken=" + validToken);

		// when
		StompSession session = customClient.connectAsync(
			"ws://localhost:" + port + "/ws/chat",
			httpHeaders,
			new StompHeaders(),
			new StompSessionHandlerAdapter() {
			}
		).get(5, SECONDS);

		// then
		assertThat(session.isConnected()).isTrue();
		session.disconnect();
	}

	@Test
	@DisplayName("쿠키(토큰)가 없거나 유효하지 않으면 웹소켓 연결에 실패한다 (401)")
	void shouldFailToConnectWithInvalidToken() {
		// given
		WebSocketStompClient customClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));

		// 잘못된 쿠키 세팅
		WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
		httpHeaders.add(HttpHeaders.COOKIE, "accessToken=invalid_token_value");

		// when & then
		assertThatThrownBy(() -> customClient.connectAsync(
			"ws://localhost:" + port + "/ws/chat",
			httpHeaders,
			new StompHeaders(),
			new StompSessionHandlerAdapter() {
			}
		).get(5, SECONDS))
			.isInstanceOf(ExecutionException.class);
	}

	@Test
	@DisplayName("연결 이후 SEND 요청 시 인증 토큰이 만료/조작된 경우 메시지를 차단하고 ERROR 프레임을 반환한다")
	void shouldFailToSendWhenTokenIsInvalid() throws Exception {
		// given
		subscribeToChatQueue();
		Thread.sleep(1000);

		// when
		doThrow(new CustomJwtException(JwtErrorCode.EXPIRED_JWT_TOKEN))
			.when(jwtTokenProvider).validateAccessToken(anyString());

		Long chatId = testChat.getId();
		Map<String, Object> req = new HashMap<>();
		req.put("chatId", chatId);
		req.put("message", "만료된 토큰으로 전송 시도");

		stompSession.send("/ws/chat/send", req);

		// then
		Thread.sleep(1000);
		verify(answerClient, never()).generateAnswer(any());
	}

	@Test
	@DisplayName("정상적인 유저가 메시지를 보내면 AI 응답이 Queue로 수신된다")
	void shouldReceiveAnswerWhenMessageSent() throws InterruptedException {
		// given
		Long chatId = testChat.getId();
		String hashedChatId = hashidsUtils.encode(chatId);
		String userMessage = "Gemini에 대해 알려줘";

		Map<String, Object> req = new HashMap<>();
		req.put("chatId", hashedChatId);
		req.put("message", userMessage);

		subscribeToChatQueue();
		Thread.sleep(1000);

		// when
		stompSession.send("/ws/chat/send", req);

		// then
		Map<String, Object> received = blockingQueue.poll(10, SECONDS);

		String receivedHashedChatId = String.valueOf(received.get("chatId"));
		assertThat(hashidsUtils.decode(receivedHashedChatId)).isEqualTo(chatId);
		assertThat(received.get("success")).isEqualTo(true);
		assertThat(String.valueOf(received.get("content"))).contains("Gemini와 관련된 내용");
	}

	@Test
	@DisplayName("답변 생성 중 취소 요청 시, 작업이 중단되고 실패 메시지가 수신된다")
	void shouldReceiveErrorMessageWhenCancelled() throws InterruptedException {
		// given
		Long chatId = testChat.getId();
		String hashedChatId = hashidsUtils.encode(chatId);
		String userMessage = "취소될 질문";

		java.util.Map<String, Object> sendReq = new java.util.HashMap<>();
		sendReq.put("chatId", hashedChatId);
		sendReq.put("message", userMessage);

		// 취소용 Map 구성
		Map<String, Object> cancelReq = new java.util.HashMap<>();
		cancelReq.put("chatId", hashedChatId);

		given(answerClient.generateAnswer(any())).willAnswer(invocation -> {
			Thread.sleep(1500);
			return new RagAnswerRes("지연된 답변", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				true);
		});

		subscribeToChatQueue();
		Thread.sleep(1000);

		// when
		stompSession.send("/ws/chat/send", sendReq);
		Thread.sleep(300);
		stompSession.send("/ws/chat/cancel", cancelReq);

		// then
		Map<String, Object> received = blockingQueue.poll(10, SECONDS);

		assertThat(received).isNotNull();

		String receivedHashedChatId = String.valueOf(received.get("chatId"));
		assertThat(hashidsUtils.decode(receivedHashedChatId)).isEqualTo(chatId);

		assertThat(received.get("success")).isEqualTo(false);
		assertThat(String.valueOf(received.get("content"))).isEqualTo(userMessage);
	}
}
