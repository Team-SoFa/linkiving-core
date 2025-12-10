package com.sofa.linkiving.security.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.security.jwt.JwtKeys;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;
import com.sofa.linkiving.security.jwt.error.JwtErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class StompHandler implements ChannelInterceptor {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

			String authorizationHeader = accessor.getFirstNativeHeader(JwtKeys.Headers.AUTHORIZATION);
			String token = null;

			if (authorizationHeader != null && authorizationHeader.startsWith(JwtKeys.Headers.BEARER_PREFIX)) {
				token = authorizationHeader.substring(JwtKeys.Headers.BEARER_PREFIX.length());
			}

			try {
				if (token == null) {
					throw new BusinessException(JwtErrorCode.EMPTY_TOKEN);
				}

				if (jwtTokenProvider.validateAccessToken(token)) {
					Authentication authentication = jwtTokenProvider.getAuthentication(token);
					accessor.setUser(authentication);
				}

			} catch (BusinessException e) {
				throw new MessagingException(e.getMessage());

			} catch (Exception e) {
				throw new MessagingException("서버 내부 오류로 연결에 실패했습니다.");
			}
		}

		return message;
	}
}
