package com.sofa.linkiving.security.auth.handler;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sofa.linkiving.global.config.CookieProperties;
import com.sofa.linkiving.security.auth.config.OAuth2Properties;
import com.sofa.linkiving.security.jwt.JwtProperties;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;
	private final OAuth2Properties oauth2Properties;
	private final JwtProperties jwtProperties;
	private final CookieProperties cookieProperties;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");

		String accessToken = jwtTokenProvider.createAccessToken(email);
		String refreshToken = jwtTokenProvider.createRefreshToken(email);

		int accessExp = (int)(jwtProperties.accessTokenValidTime() / 1000);
		int refreshExp = (int)(jwtProperties.refreshTokenValidTime() / 1000);

		addCookie(request, response, "accessToken", accessToken, accessExp);
		addCookie(request, response, "refreshToken", refreshToken, refreshExp);

		String targetUrl = oauth2Properties.successRedirectUrl();
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}

	private void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
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
