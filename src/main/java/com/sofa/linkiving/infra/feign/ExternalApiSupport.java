package com.sofa.linkiving.infra.feign;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;

import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.global.logging.ExternalApiLogger;

import io.micrometer.core.instrument.Counter;

public final class ExternalApiSupport {

	private ExternalApiSupport() {
	}

	public static BusinessException handleFailure(String client, String operation, Long linkId,
		Counter failureCounter, long startNanos, Throwable throwable) {
		Throwable cause = unwrap(throwable);
		BusinessException businessException = (cause instanceof BusinessException be) ? be : null;

		failureCounter.increment();
		ExternalApiLogger.client(client, operation)
			.detail("linkId", linkId)
			.elapsedMs(elapsedMs(startNanos))
			.errorCode(businessException != null ? businessException.getErrorCode().getCode() : null)
			.cause(cause)
			.failure();

		return businessException != null
			? businessException
			: new BusinessException(ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);
	}

	public static long elapsedMs(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000L;
	}

	private static Throwable unwrap(Throwable throwable) {
		Throwable current = throwable;
		while ((current instanceof NoFallbackAvailableException
			|| current instanceof ExecutionException
			|| current instanceof CompletionException)
			&& current.getCause() != null
			&& current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}
}
