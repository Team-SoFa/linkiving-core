package com.sofa.linkiving.security.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.code.CommonErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.security.annotation.AuthMember;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@Component
public class AuthMemberArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(AuthMember.class)
			&& Member.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || authentication.getPrincipal() == null || "anonymousUser".equals(
			authentication.getPrincipal())) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}

		Object principal = authentication.getPrincipal();

		if (principal instanceof CustomMemberDetail userDetails) {
			return userDetails.member();
		}

		throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
	}
}
