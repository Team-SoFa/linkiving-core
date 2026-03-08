package com.sofa.linkiving.domain.link.error;

import org.springframework.http.HttpStatus;

import com.sofa.linkiving.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SummaryErrorCode implements ErrorCode {

	SUMMARY_NOT_FOUND(HttpStatus.BAD_REQUEST, "S-001", "요약 정보를 찾을 수 없습니다."),
	ALREADY_PROCESSING(HttpStatus.CONFLICT, "S-002", "요약 작업이 진행중입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
