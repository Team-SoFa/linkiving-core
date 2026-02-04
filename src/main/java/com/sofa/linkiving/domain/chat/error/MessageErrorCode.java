package com.sofa.linkiving.domain.chat.error;

import org.springframework.http.HttpStatus;

import com.sofa.linkiving.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageErrorCode implements ErrorCode {
	MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MS-001", "메세지를 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
