package com.sofa.linkiving.global.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sofa.linkiving.global.config.CookieProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CookieUtils {

	private final CookieProperties cookieProperties;

	public void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
		int maxAge) {
		String domain = request.getServerName();
		boolean isLocal = "localhost".equals(domain) || "127.0.0.1".equals(domain);
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
			.path("/")
			.maxAge(maxAge)
			.httpOnly(!isLocal)
			.secure(!isLocal)
			// TODO: Review security implications of SameSite=None (CSRF risk) before finalizing.
			.sameSite(isLocal ? "Lax" : "None");

		if (!isLocal) {
			String cookieDomain = cookieProperties.domain();
			if (StringUtils.hasText(cookieDomain)) {
				builder.domain(cookieDomain);
			}
		}

		ResponseCookie cookie = builder.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
