package com.sofa.linkiving.infra.feign;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;

import com.sofa.linkiving.global.error.exception.BusinessException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ExternalApiSupportTest {

	private Counter failureCounter;

	@BeforeEach
	void setUp() {
		failureCounter = new SimpleMeterRegistry().counter("test.failures");
	}

	@Test
	@DisplayName("원인이 BusinessException 이면 그대로 반환하고 카운터를 증가시킨다")
	void handleFailure_businessExceptionCause() {
		BusinessException cause = new BusinessException(ExternalApiErrorCode.EXTERNAL_API_UNAUTHORIZED);

		BusinessException result = ExternalApiSupport.handleFailure(
			"summary", "initialSummary", 1L, failureCounter, System.nanoTime(), cause);

		assertThat(result).isSameAs(cause);
		assertThat(failureCounter.count()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("원인이 일반 예외면 통신 오류로 변환한다")
	void handleFailure_genericCause() {
		BusinessException result = ExternalApiSupport.handleFailure(
			"summary", "op", 1L, failureCounter, System.nanoTime(), new RuntimeException("boom"));

		assertThat(result.getErrorCode()).isEqualTo(ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);
	}

	@Test
	@DisplayName("NoFallbackAvailableException 으로 감싼 BusinessException 을 언랩한다")
	void handleFailure_unwrapsNoFallback() {
		BusinessException cause = new BusinessException(ExternalApiErrorCode.EXTERNAL_API_TIMEOUT);
		Throwable wrapped = new NoFallbackAvailableException("no fallback", cause);

		BusinessException result = ExternalApiSupport.handleFailure(
			"answer", "generateAnswer", null, failureCounter, System.nanoTime(), wrapped);

		assertThat(result).isSameAs(cause);
	}

	@Test
	@DisplayName("이중 래핑(NoFallback → ExecutionException)된 BusinessException 도 끝까지 언랩한다")
	void handleFailure_unwrapsDoubleWrapped() {
		BusinessException cause = new BusinessException(ExternalApiErrorCode.EXTERNAL_API_TIMEOUT);
		Throwable wrapped = new NoFallbackAvailableException("no fallback", new ExecutionException(cause));

		BusinessException result = ExternalApiSupport.handleFailure(
			"summary", "op", 1L, failureCounter, System.nanoTime(), wrapped);

		assertThat(result).isSameAs(cause);
	}

	@Test
	@DisplayName("래핑된 원인이 일반 예외면 통신 오류로 변환한다")
	void handleFailure_unwrapsToGeneric() {
		Throwable wrapped = new NoFallbackAvailableException("no fallback", new RuntimeException("boom"));

		BusinessException result = ExternalApiSupport.handleFailure(
			"summary", "op", 1L, failureCounter, System.nanoTime(), wrapped);

		assertThat(result.getErrorCode()).isEqualTo(ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);
	}

	@Test
	@DisplayName("elapsedMs 는 0 이상을 반환한다")
	void elapsedMs_nonNegative() {
		assertThat(ExternalApiSupport.elapsedMs(System.nanoTime())).isGreaterThanOrEqualTo(0L);
	}
}
