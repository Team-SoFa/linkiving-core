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
	INVALID_URL(HttpStatus.BAD_REQUEST, "L-003", "유효하지 않은 URL 형식입니다."),
	INVALID_URL_PROTOCOL(HttpStatus.BAD_REQUEST, "L-004", "허용되지 않은 프로토콜입니다. http 또는 https만 사용 가능합니다."),
	INVALID_URL_PRIVATE_IP(HttpStatus.BAD_REQUEST, "L-005", "내부 네트워크 주소는 접근할 수 없습니다."),
	SUMMARY_NOT_FOUND(HttpStatus.BAD_REQUEST, "L-010", "요약 정보를 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
