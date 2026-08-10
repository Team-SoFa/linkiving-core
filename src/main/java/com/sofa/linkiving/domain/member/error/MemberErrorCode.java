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
	INCORRECT_PASSWORD(HttpStatus.BAD_REQUEST, "M-003", "잘못된 비밀번호입니다."),
	TERMS_AGREEMENT_REQUIRED(HttpStatus.FORBIDDEN, "M-004", "약관 동의가 필요합니다."),
	TERMS_ALREADY_AGREED(HttpStatus.BAD_REQUEST, "M-005", "이미 약관 동의가 완료되었습니다."),
	INVALID_TERMS_VERSION(HttpStatus.BAD_REQUEST, "M-006", "유효하지 않은 약관 버전입니다."),
	WITHDRAWAL_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "M-007", "회원 탈퇴 기능을 사용할 수 없습니다."),
	WITHDRAWAL_CONFIRMATION_REQUIRED(HttpStatus.BAD_REQUEST, "M-008", "회원 탈퇴 확인이 필요합니다."),
	RECENT_AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "M-009", "회원 탈퇴를 위해 다시 로그인해 주세요."),
	WITHDRAWAL_IN_PROGRESS(HttpStatus.CONFLICT, "M-010", "회원 탈퇴가 진행 중입니다."),
	MEMBER_ID_MISMATCH(HttpStatus.CONFLICT, "M-011", "회원 식별자가 일치하지 않습니다."),
	AI_SERVICE_AUTH_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "M-012", "AI 서비스 인증이 설정되지 않았습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
