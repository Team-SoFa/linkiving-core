package com.sofa.linkiving.global.fillter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Set<String> SENSITIVE_HEADERS = Set.of(
		"authorization",
		"cookie",
		"set-cookie",
		"proxy-authorization",
		"x-auth-token",
		"x-api-key"
	);

	private static final Set<String> SENSITIVE_BODY_FIELDS = Set.of(
		"email",
		"password",
		"accessToken",
		"refreshToken",
		"token",
		"secret"
	);

	private static final Set<String> SENSITIVE_QUERY_PARAMS = Set.of(
		"code",
		"state"
	);

	private static final Pattern JWT_PATTERN =
		Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

	private static final int MAX_BODY_LENGTH = 2000;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		HttpServletRequest requestToUse = isLoggableBody(request.getContentType())
			? new ContentCachingRequestWrapper(request)
			: request;

		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

		long startNs = System.nanoTime();
		try {
			filterChain.doFilter(requestToUse, responseWrapper);
		} finally {
			logRequestDetails(requestToUse, responseWrapper, startNs);
			responseWrapper.copyBodyToResponse();
		}
	}

	private void logRequestDetails(HttpServletRequest request, ContentCachingResponseWrapper response, long startNs) {
		String uri = request.getRequestURI();

		long tookMs = (System.nanoTime() - startNs) / 1_000_000;
		String query = maskQueryString(request.getQueryString());

		log.info("[API] {} {}{} -> {} ({}ms) ip={} ua=\"{}\"",
			request.getMethod(),
			uri,
			query,
			response.getStatus(),
			tookMs,
			clientIp(request),
			request.getHeader("User-Agent"));

		if (log.isDebugEnabled()) {
			log.debug("[API REQUEST BODY] {} {} body={}", request.getMethod(), uri, requestBody(request));
			log.debug("[API HEADERS] {} {} {}", request.getMethod(), uri, maskedHeaders(request));
		}
	}

	private String maskQueryString(String queryString) {
		if (queryString == null || queryString.isBlank()) {
			return "";
		}

		StringBuilder sb = new StringBuilder("?");
		String[] pairs = queryString.split("&");
		for (int i = 0; i < pairs.length; i++) {
			String pair = pairs[i];
			int eq = pair.indexOf('=');
			if (eq > 0) {
				String key = pair.substring(0, eq);
				if (SENSITIVE_QUERY_PARAMS.contains(key.toLowerCase())) {
					sb.append(key).append("=***");
				} else {
					sb.append(pair);
				}
			} else {
				sb.append(pair);
			}
			if (i < pairs.length - 1) {
				sb.append("&");
			}
		}
		return sb.toString();
	}

	private String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			return realIp;
		}
		return request.getRemoteAddr();
	}

	private String maskedHeaders(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String value = SENSITIVE_HEADERS.contains(name.toLowerCase())
				? "***MASKED***"
				: request.getHeader(name);
			sb.append(name).append("=").append(value).append("; ");
		}
		return sb.toString();
	}

	private String requestBody(HttpServletRequest request) {
		if (request instanceof ContentCachingRequestWrapper wrapper) {
			return formatBody(wrapper.getContentType(), wrapper.getContentAsByteArray());
		}
		return "(skipped, content-type=" + request.getContentType() + ")";
	}

	private String formatBody(String contentType, byte[] content) {
		if (!isLoggableBody(contentType)) {
			return "(skipped, content-type=" + contentType + ")";
		}
		if (content.length == 0) {
			return "(empty)";
		}
		String body = maskBody(new String(content, StandardCharsets.UTF_8));
		if (body.length() > MAX_BODY_LENGTH) {
			return body.substring(0, MAX_BODY_LENGTH) + "...(truncated, total " + body.length() + " chars)";
		}
		return body;
	}

	private boolean isLoggableBody(String contentType) {
		return contentType != null && (contentType.contains("json") || contentType.startsWith("text/"));
	}

	private String maskBody(String body) {
		String masked = body;
		for (String field : SENSITIVE_BODY_FIELDS) {
			masked = masked.replaceAll("(\"" + field + "\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
		}
		masked = JWT_PATTERN.matcher(masked).replaceAll("***JWT***");
		return masked;
	}

}
