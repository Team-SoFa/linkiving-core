package com.sofa.linkiving.global.common;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BaseResponse<T> {
	private boolean success;
	private HttpStatus status;
	private String message;
	private T data;
	private LocalDateTime timestamp;

	public static <T> BaseResponse<T> success(T data, String message) {
		return BaseResponse.<T>builder()
			.status(HttpStatus.OK)
			.success(true)
			.message(message)
			.data(data)
			.timestamp(LocalDateTime.now())
			.build();
	}

	public static <T> BaseResponse<T> noContent(String message) {
		return BaseResponse.<T>builder()
			.status(HttpStatus.NO_CONTENT)
			.success(true)
			.message(message)
			.timestamp(LocalDateTime.now())
			.build();
	}

	public static BaseResponse<String> error(HttpStatus status, String message, String errorCode) {
		return BaseResponse.<String>builder()
			.status(status)
			.success(false)
			.message(message)
			.data(errorCode)
			.timestamp(LocalDateTime.now())
			.build();
	}

	public static BaseResponse<String> internalServerError(String message, String errorCode) {
		return BaseResponse.<String>builder()
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.success(false)
			.message(message)
			.data(errorCode)
			.timestamp(LocalDateTime.now())
			.build();
	}

}
