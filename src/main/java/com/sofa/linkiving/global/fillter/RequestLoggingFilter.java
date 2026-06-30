package com.sofa.linkiving.global.fillter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.sofa.linkiving.global.logging.AccessLogger;
import com.sofa.linkiving.global.logging.LogContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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
		"token"
	);

	private static final Set<String> SENSITIVE_QUERY_PARAMS = Set.of(
		"code",
		"state"
	);

	private static final Pattern JWT_PATTERN =
		Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

	private static final String[] SKIP_PATH_PREFIXES = {
		"/actuator",
		"/favicon.ico",
		"/swagger",
		"/v3/api-docs",
		"/health-check",
		"/h2-console"
	};

	private static final int MAX_BODY_LENGTH = 2000;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		HttpServletRequest requestToUse = isLoggableBody(request.getContentType())
			? new ContentCachingRequestWrapper(request)
			: request;

		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

		long startedAt = System.nanoTime();
		String requestId = resolveRequestId(request);
		String traceId = resolveTraceId(request, requestId);

		try (LogContext.MdcScope ignored = LogContext.withRequest(requestId, traceId)) {
			filterChain.doFilter(requestToUse, responseWrapper);
		} finally {
			logRequestDetails(requestToUse, responseWrapper, startedAt);
			responseWrapper.copyBodyToResponse();
		}
	}

	private void logRequestDetails(HttpServletRequest request, ContentCachingResponseWrapper response, long startedAt) {
		String uri = request.getRequestURI();
		if (shouldSkip(uri)) {
			return;
		}

		long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
		AccessLogger.info(
			"requestId={} method={} path={} status={} latencyMs={}",
			LogContext.snapshot().get(LogContext.REQUEST_ID),
			request.getMethod(),
			uri,
			response.getStatus(),
			latencyMs
		);

		if (log.isDebugEnabled()) {
			String uriWithQuery = uri + maskQueryString(request.getQueryString());
			log.debug("[API REQUEST BODY] {} {} body={}", request.getMethod(), uriWithQuery, requestBody(request));
			log.debug("[API HEADERS] {} {} ip={} ua=\"{}\" {}",
				request.getMethod(),
				uriWithQuery,
				clientIp(request),
				request.getHeader("User-Agent"),
				maskedHeaders(request));
			log.debug("[API RESPONSE] {} {} -> {} body={}",
				request.getMethod(),
				uriWithQuery,
				response.getStatus(),
				responseBody(response));
		}
	}

	private boolean shouldSkip(String uri) {
		for (String prefix : SKIP_PATH_PREFIXES) {
			if (uri.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private String resolveRequestId(HttpServletRequest request) {
		String requestId = request.getHeader("X-Request-Id");
		if (hasText(requestId)) {
			return requestId;
		}
		return UUID.randomUUID().toString();
	}

	private String resolveTraceId(HttpServletRequest request, String fallback) {
		String traceparent = request.getHeader("traceparent");
		if (hasText(traceparent)) {
			String[] parts = traceparent.split("-");
			if (parts.length >= 2 && hasText(parts[1])) {
				return parts[1];
			}
		}

		String xB3TraceId = request.getHeader("X-B3-TraceId");
		if (hasText(xB3TraceId)) {
			return xB3TraceId;
		}

		String xTraceId = request.getHeader("X-Trace-Id");
		if (hasText(xTraceId)) {
			return xTraceId;
		}

		return fallback;
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

	private String responseBody(ContentCachingResponseWrapper response) {
		return formatBody(response.getContentType(), response.getContentAsByteArray());
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

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
