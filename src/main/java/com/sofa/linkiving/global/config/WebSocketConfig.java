package com.sofa.linkiving.global.config;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.sofa.linkiving.security.config.StompHandler;
import com.sofa.linkiving.security.resolver.AuthMemberWebsocketArgumentResolver;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompHandler stompHandler;
	private final AuthMemberWebsocketArgumentResolver authMemberWebsocketArgumentResolver;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/queue");
		config.setApplicationDestinationPrefixes("/ws/chat", "/ws/link");

		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		HandshakeInterceptor cookieInterceptor = new HandshakeInterceptor() {
			@Override
			public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
				WebSocketHandler wsHandler, Map<String, Object> attributes) {
				if (request instanceof ServletServerHttpRequest servletRequest) {
					Cookie[] cookies = servletRequest.getServletRequest().getCookies();
					if (cookies != null) {
						for (Cookie cookie : cookies) {
							if ("accessToken".equals(cookie.getName())) {
								attributes.put("accessToken", cookie.getValue()); // 세션 주머니에 보관!
								break;
							}
						}
					}
				}
				return true;
			}

			@Override
			public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
				WebSocketHandler wsHandler, Exception exception) {
			}
		};

		registry.addEndpoint("/ws/chat")
			.setAllowedOriginPatterns("*")
			.addInterceptors(cookieInterceptor)
			.withSockJS();

		registry.addEndpoint("/ws/link")
			.setAllowedOriginPatterns("*")
			.addInterceptors(cookieInterceptor)
			.withSockJS();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompHandler);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(authMemberWebsocketArgumentResolver);
	}
}
