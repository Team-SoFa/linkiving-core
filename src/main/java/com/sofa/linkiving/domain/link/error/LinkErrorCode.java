package com.sofa.linkiving.domain.link.error;

import org.springframework.http.HttpStatus;

import com.sofa.linkiving.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LinkErrorCode implements ErrorCode {

	LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "L-001", "링크를 찾을 수 없습니다."),
	DUPLICATE_URL(HttpStatus.BAD_REQUEST, "L-002", "이미 저장된 URL입니다."),
	SUMMARY_NOT_FOUND(HttpStatus.BAD_REQUEST, "L-010", "요약 정보를 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
