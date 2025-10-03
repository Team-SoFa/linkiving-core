package com.sofa.linkiving.global.error.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.global.error.code.CommonErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.global.error.util.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/* =========================
	  도메인 비즈니스 예외
	  ========================= */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<BaseResponse<String>> handleBusinessException(BusinessException ex) {
		log.error(ex.getMessage(), ex);
		return ErrorResponse.build(ex.getErrorCode());
	}

	/* =========================
       검증/바인딩 예외
       ========================= */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<BaseResponse<String>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		log.error(ex.getMessage(), ex);
		return ErrorResponse.build(CommonErrorCode.INVALID_INPUT_VALUE);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<BaseResponse<String>> handleConstraintViolation(ConstraintViolationException ex) {
		log.error(ex.getMessage(), ex);
		return ErrorResponse.build(CommonErrorCode.INVALID_INPUT_VALUE);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<BaseResponse<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		log.error(ex.getMessage(), ex);
		return ErrorResponse.build(CommonErrorCode.TYPE_MISMATCH);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<BaseResponse<String>> handleMissingParam(MissingServletRequestParameterException ex) {
		log.error(ex.getMessage(), ex);
		return ErrorResponse.build(CommonErrorCode.MISSING_REQUEST_PARAMS);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<BaseResponse<String>> handleNotReadable(HttpMessageNotReadableException ex) {
		log.error(ex.getMessage(), ex);
		return ErrorResponse.build(CommonErrorCode.BAD_REQUEST);
	}

	/* =========================
	 HTTP 관련 예외
	 ========================= */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<BaseResponse<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		log.error(ex.getMessage(), ex);
		return ErrorResponse.build(CommonErrorCode.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<BaseResponse<String>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
		log.error(ex.getMessage(), ex);
		return ErrorResponse.build(CommonErrorCode.HTTP_MEDIA_NOT_SUPPORT);
	}

	/* =========================
       그 외 모든 예외
       ========================= */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<String>> handleException(Exception ex) {
		log.error(ex.getMessage(), ex);
		return ErrorResponse.build(CommonErrorCode.INTERNAL_SERVER_ERROR);
	}

}
