package com.sofa.linkiving.domain.member.error;

import org.springframework.http.HttpStatus;

import com.sofa.linkiving.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "M-000", "유효하지 않은 이메일 형식입니다."),
	DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "M-001", "이미 존재하는 이메일입니다."),
	USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "M-002", "존재하지 않는 유저입니다."),
	INCORRECT_PASSWORD(HttpStatus.BAD_REQUEST, "M-003", "잘못된 비밀번호입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
