package com.sofa.linkiving.global.error.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.global.error.code.CommonErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.global.error.util.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException exception) {
		log.warn("No static resource: {}", exception.getResourcePath());
		return ResponseEntity.notFound().build();
	}

	/* =========================
	  도메인 비즈니스 예외
	  ========================= */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<BaseResponse<String>> handleBusinessException(BusinessException ex) {
		if (ex.getErrorCode().getStatus().is5xxServerError()) {
			log.error("BusinessException [{}]", ex.getErrorCode().getCode(), ex);
		} else {
			log.warn("BusinessException [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());
		}
		return ErrorResponse.build(ex.getErrorCode());
	}

	/* =========================
       검증/바인딩 예외 (모두 4xx)
       ========================= */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<BaseResponse<String>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		log.warn("Validation failed: {} field error(s) - fields={}",
			ex.getBindingResult().getErrorCount(),
			ex.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getField).distinct().toList());
		return ErrorResponse.build(CommonErrorCode.INVALID_INPUT_VALUE);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<BaseResponse<String>> handleConstraintViolation(ConstraintViolationException ex) {
		log.warn("Constraint violation: {}", ex.getMessage());
		return ErrorResponse.build(CommonErrorCode.INVALID_INPUT_VALUE);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<BaseResponse<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		log.warn("Type mismatch: parameter '{}'", ex.getName());
		return ErrorResponse.build(CommonErrorCode.TYPE_MISMATCH);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<BaseResponse<String>> handleMissingParam(MissingServletRequestParameterException ex) {
		log.warn("Missing request parameter: {}", ex.getParameterName());
		return ErrorResponse.build(CommonErrorCode.MISSING_REQUEST_PARAMS);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<BaseResponse<String>> handleNotReadable(HttpMessageNotReadableException ex) {
		log.warn("Malformed request body");
		return ErrorResponse.build(CommonErrorCode.BAD_REQUEST);
	}

	/* =========================
	 HTTP 관련 예외 (모두 4xx)
	 ========================= */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<BaseResponse<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		log.warn("Method not supported: {}", ex.getMethod());
		return ErrorResponse.build(CommonErrorCode.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<BaseResponse<String>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
		log.warn("Media type not supported: {}", ex.getContentType());
		return ErrorResponse.build(CommonErrorCode.HTTP_MEDIA_NOT_SUPPORT);
	}

	/* =========================
       그 외 모든 예외 (예상 못 한 5xx)
       ========================= */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<String>> handleException(Exception ex) {
		log.error("Unhandled exception", ex);
		return ErrorResponse.build(CommonErrorCode.INTERNAL_SERVER_ERROR);
	}

}
