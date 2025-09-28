package com.sofa.linkiving.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C-000", "서버 내부 오류가 발생하였습니다."),
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "C-001", "잘못된 요청입니다."),
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C-002", "유효하지 않은 입력입니다."),
	TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "C-003", "입력 타입이 일치하지 않습니다."),
	MISSING_REQUEST_PARAMS(HttpStatus.BAD_REQUEST, "C-004", "입력 파라미터가 필요합니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C-005", "로그인이 필요합니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "C-006", "리소스를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C-007", "허용되지 않은 HTTP 메소드입니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "C-008", "접근이 허용되지 않았습니다."),
	NULL_POINTER(HttpStatus.INTERNAL_SERVER_ERROR, "C-009", "NullPointerException이 발생했습니다."),
	HTTP_MEDIA_NOT_SUPPORT(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C-010", "지원하지 않는 미디어입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
