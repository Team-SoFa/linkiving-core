package com.sofa.linkiving.infra.feign;

import com.sofa.linkiving.global.error.exception.BusinessException;

public class EmptyAiResponseException extends BusinessException {
	public EmptyAiResponseException() {
		super(ExternalApiErrorCode.EXTERNAL_API_INVALID_RESPONSE);
	}
}
