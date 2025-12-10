package com.sofa.linkiving.domain.chat.integration;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.chat.service.MessageService;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebSocketChatIntegrationTest {

	@LocalServerPort
	private int port;

	private WebSocketStompClient stompClient;

	@Autowired
	private MessageService messageService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ChatRepository chatRepository;

	@MockitoBean
	private RedisService redisService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider; // 실제 토큰 생성 로직 사용 (또는 MockBean)

	private Chat savedChat;
	private String validToken;

	@BeforeEach
	void setUp() {
		// 1. WebSocket Client 설정
		StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();
		WebSocketTransport webSocketTransport = new WebSocketTransport(standardWebSocketClient);
		List<Transport> transports = List.of(webSocketTransport);
		SockJsClient sockJsClient = new SockJsClient(transports);

		stompClient = new WebSocketStompClient(sockJsClient);
		stompClient.setMessageConverter(new StringMessageConverter());

		// 2. MockAiController 연결을 위한 WebClient 주소 조작 (핵심)
		String testUrl = "http://localhost:" + port + "/mock/ai";
		WebClient testWebClient = WebClient.create(testUrl);
		ReflectionTestUtils.setField(messageService, "webClient", testWebClient);

		// 3. 테스트 데이터 생성
		String uniqueEmail = "socket_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

		Member savedMember = memberRepository.save(Member.builder()
			.email(uniqueEmail)
			.password("password")
			.build());

		savedChat = chatRepository.save(Chat.builder()
			.member(savedMember)
			.title("test")
			.build());

		// 4. 유효한 토큰 생성 (StompHandler 통과용)
		validToken = jwtTokenProvider.createAccessToken(savedMember.getEmail());
	}

	@Test
	@DisplayName("메시지 전송 시 MockAiController를 통해 스트리밍 답변 수신")
	void shouldReceiveStreamingResponseWhenSendMessage() throws Exception {
		// given
		String wsUrl = String.format("ws://localhost:%d/ws/chat", port);
		StompHeaders headers = new StompHeaders();
		headers.add("Authorization", "Bearer " + validToken);

		WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();

		StompSession session = stompClient.connectAsync(wsUrl, handshakeHeaders, headers,
				new StompSessionHandlerAdapter() {
				})
			.get(5, TimeUnit.SECONDS);

		Long chatId = savedChat.getId();
		String userMessage = "테스트 질문";
		BlockingQueue<String> queue = new LinkedBlockingQueue<>();

		// when: 구독 (/topic/chat/{chatId})
		session.subscribe("/topic/chat/" + chatId, new StompFrameHandler() {
			@NotNull
			@Override
			public Type getPayloadType(@NotNull StompHeaders headers) {
				return String.class;
			}

			@Override
			public void handleFrame(@NotNull StompHeaders headers, Object payload) {
				queue.add((String)payload);
			}
		});

		// when: 메시지 전송
		session.send("/ws/chat/send/" + chatId, userMessage);

		// then: MockAiController가 보내는 응답 검증
		String response = queue.poll(5, TimeUnit.SECONDS);

		assertThat(response).isNotNull();
		assertThat(response).startsWith("안");
	}

	@Test
	@DisplayName("취소 요청 시 GENERATION_CANCELLED 메시지 수신")
	void shouldReceiveCancelledMessageWhenCancelRequest() throws Exception {
		// given
		String wsUrl = String.format("ws://localhost:%d/ws/chat", port);
		StompHeaders headers = new StompHeaders();
		headers.add("Authorization", "Bearer " + validToken);

		WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();

		StompSession session = stompClient.connectAsync(wsUrl, handshakeHeaders, headers,
				new StompSessionHandlerAdapter() {
				})
			.get(5, TimeUnit.SECONDS);

		Long chatId = savedChat.getId();
		BlockingQueue<String> queue = new LinkedBlockingQueue<>();

		session.subscribe("/topic/chat/" + chatId, new StompFrameHandler() {
			@NotNull
			@Override
			public Type getPayloadType(@NotNull StompHeaders headers) {
				return String.class;
			}

			@Override
			public void handleFrame(@NotNull StompHeaders headers, Object payload) {
				queue.add((String)payload);
			}
		});

		// when: 취소 요청 전송
		session.send("/ws/chat/cancel/" + chatId, "");

		// then
		String response = queue.poll(5, TimeUnit.SECONDS);
		assertThat(response).isEqualTo("GENERATION_CANCELLED");
	}
}
