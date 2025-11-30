package com.sofa.linkiving.infra.feign;

import org.springframework.http.HttpStatus;

import com.sofa.linkiving.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalApiErrorCode implements ErrorCode {
	EXTERNAL_API_COMMUNICATION_ERROR(HttpStatus.BAD_GATEWAY, "E_000", "외부 API 통신 중 오류가 발생했습니다."),
	EXTERNAL_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "E_001", "외부 API 응답 시간이 초과되었습니다."),
	EXTERNAL_API_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "E_002", "외부 API 응답 포맷이 올바르지 않습니다."),
	EXTERNAL_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E_003", "외부 API 인증에 실패했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
