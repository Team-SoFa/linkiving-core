package com.sofa.linkiving.domain.chat.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.infra.feign.EmptyAiResponseException;
import com.sofa.linkiving.infra.feign.ExternalApiErrorCode;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagAnswerClient 단위 테스트")
class RagAnswerClientTest {

	@Mock
	private RagAnswerFeign ragAnswerFeign;
	private RagAnswerClient ragAnswerClient;
	private SimpleMeterRegistry meterRegistry;

	@BeforeEach
	void setUp() {
		meterRegistry = new SimpleMeterRegistry();
		ragAnswerClient = new RagAnswerClient(ragAnswerFeign, meterRegistry);
		ReflectionTestUtils.invokeMethod(ragAnswerClient, "initCounters");
	}

	private double counterCount(String result) {
		return meterRegistry.counter("ai.client.calls", "client", "answer", "operation", "generate", "result", result)
			.count();
	}

	@Test
	@DisplayName("Feign 응답이 정상일 경우 리스트의 첫 번째 요소를 반환한다")
	void shouldReturnFirstElement_WhenGenerateAnswerSuccess() {
		// given
		RagAnswerReq req = mock(RagAnswerReq.class);
		RagAnswerRes expectedRes = mock(RagAnswerRes.class);
		given(ragAnswerFeign.generateAnswer(any(RagAnswerReq.class)))
			.willReturn(List.of(expectedRes));

		// when
		RagAnswerRes actualRes = ragAnswerClient.generateAnswer(req);

		// then
		assertThat(actualRes).isEqualTo(expectedRes);
		assertThat(counterCount("success")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Feign 요청 중 예외가 발생하면 통신 오류 예외로 전환하고 failure 로 집계한다")
	void shouldThrowCommunicationError_WhenGenerateAnswerThrowsException() {
		// given
		RagAnswerReq req = mock(RagAnswerReq.class);
		given(ragAnswerFeign.generateAnswer(any(RagAnswerReq.class)))
			.willThrow(new RuntimeException("AI Server Error"));

		// when & then
		assertThatThrownBy(() -> ragAnswerClient.generateAnswer(req))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);
		assertThat(counterCount("failure")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Feign 응답이 비어있으면 EmptyAiResponseException 을 던지고 empty 로 집계한다")
	void shouldThrowEmptyResponse_WhenResponseIsEmpty() {
		// given
		RagAnswerReq req = mock(RagAnswerReq.class);
		given(ragAnswerFeign.generateAnswer(any(RagAnswerReq.class)))
			.willReturn(Collections.emptyList());

		// when & then
		assertThatThrownBy(() -> ragAnswerClient.generateAnswer(req))
			.isInstanceOf(EmptyAiResponseException.class);
		assertThat(counterCount("empty")).isEqualTo(1.0);
	}
}
