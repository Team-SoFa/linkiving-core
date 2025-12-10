package com.sofa.linkiving.security.resolver;

import java.security.Principal;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.code.CommonErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.security.annotation.AuthMember;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@Component
public class AuthMemberWebsocketArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(AuthMember.class)
			&& Member.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Principal principal = SimpMessageHeaderAccessor.getUser(message.getHeaders());

		if (principal instanceof AbstractAuthenticationToken authentication) {
			Object userDetails = authentication.getPrincipal();

			if (userDetails instanceof CustomMemberDetail customMemberDetail) {
				return customMemberDetail.member();
			}
		}

		throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
	}
}
