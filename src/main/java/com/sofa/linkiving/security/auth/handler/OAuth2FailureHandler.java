package com.sofa.linkiving.security.auth.handler;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.sofa.linkiving.global.error.code.ErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.security.auth.code.AuthErrorCode;
import com.sofa.linkiving.security.auth.config.OAuth2Properties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private final OAuth2Properties oauth2Properties;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws IOException {

		ErrorCode errorCode = AuthErrorCode.LOGIN_FAILED;
		Throwable cause = exception.getCause();

		if (cause instanceof BusinessException businessException) {
			errorCode = businessException.getErrorCode();

		} else if (exception instanceof OAuth2AuthenticationException oauthException) {
			OAuth2Error error = oauthException.getError();
			errorCode = determineAuthErrorCode(error.getErrorCode());
		}

		String targetUrl = UriComponentsBuilder.fromUriString(oauth2Properties.failureRedirectUrl())
			.queryParam("code", errorCode.getCode())
			.build().toUriString();

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}

	private AuthErrorCode determineAuthErrorCode(String providerErrorCode) {
		return switch (providerErrorCode) {
			case "access_denied" -> AuthErrorCode.USER_CANCELLED;
			case "invalid_client", "invalid_request" -> AuthErrorCode.INVALID_SOCIAL_PROVIDER;
			case "server_error", "temporarily_unavailable" -> AuthErrorCode.PROVIDER_SERVER_ERROR;
			default -> AuthErrorCode.LOGIN_FAILED;
		};
	}
}
