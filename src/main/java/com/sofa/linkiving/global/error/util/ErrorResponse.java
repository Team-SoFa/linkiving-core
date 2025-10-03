package com.sofa.linkiving.global.error.util;

import org.springframework.http.ResponseEntity;

import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.global.error.code.ErrorCode;

public class ErrorResponse {
	private ErrorResponse() {
	}

	public static ResponseEntity<BaseResponse<String>> build(ErrorCode code) {
		return ResponseEntity.status(code.getStatus())
			.body(BaseResponse.error(code));
	}
}
