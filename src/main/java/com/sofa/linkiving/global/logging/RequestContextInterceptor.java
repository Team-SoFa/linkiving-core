package com.sofa.linkiving.global.logging;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestContextInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof CustomMemberDetail memberDetail) {
			LogContext.put(LogContext.MEMBER_ID, memberDetail.member().getId());
		}

		Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (attribute instanceof Map<?, ?> pathVariables) {
			putIfPresent(LogContext.LINK_ID, pathVariables.get("linkId"));
			putIfPresent(LogContext.CHAT_ID, pathVariables.get("chatId"));
			putDomainIdByRoute(request, pathVariables.get("id"));
		}

		putIfPresent(LogContext.LINK_ID, request.getParameter("linkId"));
		putIfPresent(LogContext.CHAT_ID, request.getParameter("chatId"));
		return true;
	}

	private void putDomainIdByRoute(HttpServletRequest request, Object id) {
		if (id == null) {
			return;
		}

		String uri = request.getRequestURI();
		if (uri.startsWith("/v1/links/")) {
			LogContext.put(LogContext.LINK_ID, id);
		}
	}

	private void putIfPresent(String key, Object value) {
		if (value == null) {
			return;
		}
		LogContext.put(key, value);
	}
}
