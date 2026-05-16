package com.sofa.linkiving.security.auth.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.global.util.CookieUtils;
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
	private final CookieUtils cookieUtils;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");

		String accessToken = jwtTokenProvider.createAccessToken(email);
		String refreshToken = jwtTokenProvider.createRefreshToken(email);

		int accessExp = (int)(jwtProperties.accessTokenValidTime() / 1000);
		int refreshExp = (int)(jwtProperties.refreshTokenValidTime() / 1000);

		cookieUtils.addCookie(request, response, "accessToken", accessToken, accessExp);
		cookieUtils.addCookie(request, response, "refreshToken", refreshToken, refreshExp);

		String targetUrl = oauth2Properties.successRedirectUrl();
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}
