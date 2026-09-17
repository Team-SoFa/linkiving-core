package com.sofa.linkiving.infra.feign;

import com.sofa.linkiving.global.error.exception.BusinessException;

import feign.Response;
import feign.codec.ErrorDecoder;

public class GlobalFeignErrorDecoder implements ErrorDecoder {
	@Override
	public Exception decode(String methodKey, Response response) {
		ExternalApiErrorCode errorCode = mapErrorCode(response.status());
		if (response.status() >= 400 && response.status() < 500
			&& response.status() != 408 && response.status() != 429) {
			return new NonRetryableExternalApiException(errorCode, response.status());
		}
		return new BusinessException(errorCode);
	}

	private ExternalApiErrorCode mapErrorCode(int status) {
		if (status == 401) {
			return ExternalApiErrorCode.EXTERNAL_API_UNAUTHORIZED;
		}

		if (status == 408 || status == 504) {
			return ExternalApiErrorCode.EXTERNAL_API_TIMEOUT;
		}

		if (status == 429) {
			return ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE;
		}

		if (status >= 500) {
			return ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR;
		}

		return ExternalApiErrorCode.EXTERNAL_API_INVALID_RESPONSE;
	}

}
