package com.sofa.linkiving.security.jwt.entrypoint;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.global.error.code.CommonErrorCode;
import com.sofa.linkiving.global.error.code.ErrorCode;
import com.sofa.linkiving.global.error.util.ErrorResponse;
import com.sofa.linkiving.security.jwt.error.JwtErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {

		String exception = (String)request.getAttribute("exception");

		if (exception == null) {
			setErrorResponse(response, CommonErrorCode.UNAUTHORIZED);
		} else if (exception.equals(JwtErrorCode.INVALID_JWT_TOKEN.getCode())) {
			setErrorResponse(response, JwtErrorCode.INVALID_JWT_TOKEN);
		} else if (exception.equals(JwtErrorCode.EXPIRED_JWT_TOKEN.getCode())) {
			setErrorResponse(response, JwtErrorCode.EXPIRED_JWT_TOKEN);
		} else {
			setErrorResponse(response, CommonErrorCode.UNAUTHORIZED);
		}
	}

	private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		if (response.isCommitted()) {
			return;
		}
		ResponseEntity<BaseResponse<String>> entity = ErrorResponse.build(errorCode);

		response.setStatus(errorCode.getStatus().value());
		response.setContentType("application/json;charset=UTF-8");

		objectMapper.writeValue(response.getWriter(), entity.getBody());
	}
}
