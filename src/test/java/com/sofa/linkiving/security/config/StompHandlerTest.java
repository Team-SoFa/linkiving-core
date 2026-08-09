package com.sofa.linkiving.security.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.security.jwt.JwtKeys;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class StompHandlerTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private MessageChannel messageChannel;

	@Test
	void shouldBlockPendingTermsMemberOnConnect() {
		// given
		String token = "access-token";
		StompHandler stompHandler = new StompHandler(jwtTokenProvider);
		Message<byte[]> message = connectMessage(token);

		given(jwtTokenProvider.validateAccessToken(token)).willReturn(true);
		given(jwtTokenProvider.getClaim(token, JwtKeys.Claims.MEMBER_STATUS))
			.willReturn(MemberStatus.PENDING_TERMS.name());

		// when & then
		assertThatThrownBy(() -> stompHandler.preSend(message, messageChannel))
			.isInstanceOf(MessagingException.class);

		verify(jwtTokenProvider, never()).getAuthentication(token);
	}

	@Test
	void preSend_blocksConnectWhenTokenIsNotAccessToken() {
		// given
		StompHandler handler = new StompHandler(jwtTokenProvider);
		String token = "refresh.token";
		Message<byte[]> message = connectMessage(token);

		given(jwtTokenProvider.validateAccessToken(token)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> handler.preSend(message, messageChannel))
			.isInstanceOf(MessagingException.class);

		then(jwtTokenProvider).should(never()).getAuthentication(anyString());
	}

	private Message<byte[]> connectMessage(String token) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setSessionAttributes(new HashMap<>());
		accessor.addNativeHeader(JwtKeys.Headers.AUTHORIZATION, JwtKeys.Headers.BEARER_PREFIX + token);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
