package com.sofa.linkiving.domain.chat.error;

import org.springframework.http.HttpStatus;

import com.sofa.linkiving.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

	CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "C-001", "채팅을 찾을 수 없습니다."),
	ALREADY_GENERATING(HttpStatus.BAD_REQUEST, "C-002", "현재 답변이 생성 중입니다. 잠시만 기다려주세요.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
