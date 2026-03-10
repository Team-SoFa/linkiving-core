package com.sofa.linkiving.global.fillter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		ContentCachingRequestWrapper wrappingRequest = new ContentCachingRequestWrapper(request);

		filterChain.doFilter(wrappingRequest, response);

		logRequestDetails(wrappingRequest);
	}

	private void logRequestDetails(ContentCachingRequestWrapper request) {
		String method = request.getMethod();
		String url = request.getRequestURI();
		String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";

		StringBuilder logMsg = new StringBuilder();
		logMsg.append("\n[API REQUEST] ").append(method).append(" ").append(url).append(queryString).append("\n");

		logMsg.append(" > [HEADERS]\n");
		Collections.list(request.getHeaderNames()).forEach(headerName ->
			logMsg.append("   - ").append(headerName).append(": ").append(request.getHeader(headerName)).append("\n")
		);

		byte[] content = request.getContentAsByteArray();
		if (content.length > 0) {
			String body = new String(content, StandardCharsets.UTF_8);
			logMsg.append(" > [Body Data]       : ").append(body.trim()).append("\n");
		} else {
			logMsg.append(" > [Body Data]       : (Empty)\n");
		}

		logMsg.append("--------------------------------------------------------------------------------");

		log.info(logMsg.toString());
	}
}
