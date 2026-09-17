package com.sofa.linkiving.infra.feign;

import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.Getter;

@Getter
public class NonRetryableExternalApiException extends BusinessException {

	private final int upstreamStatus;

	public NonRetryableExternalApiException(ExternalApiErrorCode errorCode, int upstreamStatus) {
		super(errorCode);
		this.upstreamStatus = upstreamStatus;
	}
}
