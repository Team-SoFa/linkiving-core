package com.sofa.linkiving.security.jwt.error;

import org.springframework.http.HttpStatus;

import com.sofa.linkiving.global.error.code.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JwtErrorCode implements ErrorCode {

	INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "J-000", "유효하지 않은 jwt 토큰입니다."),
	EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "J-001", "만료된 jwt 토큰입니다."),
	UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "J-002", "지원하지 않는 JWT 토큰입니다."),
	EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "J-003", "토큰이 필요합니다."),
	CANNOT_REFRESH(HttpStatus.UNAUTHORIZED, "J-004", "토큰을 갱신할 수 없습니다."),
	INVALID_REFRESH(HttpStatus.BAD_REQUEST, "J-004", "refresh 토큰이 아닙니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
