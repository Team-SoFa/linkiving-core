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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.security.jwt.JwtKeys;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

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

	@Test
	void shouldBlockWithdrawingMemberOnConnectUsingLatestMemberState() {
		String token = "access-token";
		Message<byte[]> message = connectMessage(token);

		given(jwtTokenProvider.validateAccessToken(token)).willReturn(true);
		given(jwtTokenProvider.getClaim(token, JwtKeys.Claims.MEMBER_STATUS))
			.willReturn(MemberStatus.ACTIVE.name());
		given(jwtTokenProvider.getAuthentication(token)).willReturn(withdrawingAuthentication());

		assertThatThrownBy(() -> new StompHandler(jwtTokenProvider).preSend(message, messageChannel))
			.isInstanceOf(MessagingException.class);
	}

	@Test
	void shouldBlockWithdrawingMemberOnSendUsingLatestMemberState() {
		String token = "access-token";
		Message<byte[]> message = sendMessage(token);

		given(jwtTokenProvider.validateAccessToken(token)).willReturn(true);
		given(jwtTokenProvider.getAuthentication(token)).willReturn(withdrawingAuthentication());

		assertThatThrownBy(() -> new StompHandler(jwtTokenProvider).preSend(message, messageChannel))
			.isInstanceOf(MessagingException.class);
		then(jwtTokenProvider).should(never()).getClaim(anyString(), anyString());
	}

	private Message<byte[]> connectMessage(String token) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setSessionAttributes(new HashMap<>());
		accessor.addNativeHeader(JwtKeys.Headers.AUTHORIZATION, JwtKeys.Headers.BEARER_PREFIX + token);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private Message<byte[]> sendMessage(String token) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		HashMap<String, Object> sessionAttributes = new HashMap<>();
		sessionAttributes.put("accessToken", token);
		accessor.setSessionAttributes(sessionAttributes);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private Authentication withdrawingAuthentication() {
		Member member = Member.builder()
			.email("member@test.com")
			.password("password")
			.status(MemberStatus.WITHDRAWING)
			.build();
		CustomMemberDetail detail = new CustomMemberDetail(member, Role.USER);
		return new UsernamePasswordAuthenticationToken(detail, null, detail.getAuthorities());
	}
}
