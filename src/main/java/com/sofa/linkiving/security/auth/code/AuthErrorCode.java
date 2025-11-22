package com.sofa.linkiving.security.auth.code;

import org.springframework.http.HttpStatus;

import com.sofa.linkiving.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A-000", "로그인에 실패했습니다."),
	INVALID_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "A-001", "지원하지 않는 소셜 로그인입니다."),
	AUTHORIZATION_REQUEST_NOT_FOUND(HttpStatus.BAD_REQUEST, "A-002", "인증 요청 정보를 찾을 수 없습니다. (쿠키 누락 등)"),
	USER_CANCELLED(HttpStatus.BAD_REQUEST, "A-003", "사용자가 로그인을 취소했습니다."),
	PROVIDER_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A-100", "소셜 공급자(Google) 서버 오류입니다."),
	INTERNAL_AUTH_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A-101", "인증 처리 중 서버 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
