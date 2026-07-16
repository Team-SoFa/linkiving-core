package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.infra.feign.EmptyAiResponseException;
import com.sofa.linkiving.infra.feign.ExternalApiErrorCode;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RagAnswerClient implements AnswerClient {

	private final RagAnswerFeign ragAnswerFeign;
	private final MeterRegistry meterRegistry;

	private Counter successCounter;
	private Counter emptyCounter;
	private Counter failureCounter;

	@PostConstruct
	private void initCounters() {
		this.successCounter = buildCounter("success");
		this.emptyCounter = buildCounter("empty");
		this.failureCounter = buildCounter("failure");
	}

	private Counter buildCounter(String result) {
		return Counter.builder("ai.client.calls")
			.tag("client", "answer")
			.tag("result", result)
			.register(meterRegistry);
	}

	@Override
	public RagAnswerRes generateAnswer(RagAnswerReq request) {
		List<RagAnswerRes> ragAnswerRes;
		try {
			ragAnswerRes = ragAnswerFeign.generateAnswer(request);
		} catch (BusinessException e) {
			failureCounter.increment();
			log.warn("[AI Server] generateAnswer failed - code={}", e.getErrorCode().getCode());
			throw e;
		} catch (Exception e) {
			failureCounter.increment();
			log.warn("[AI Server] generateAnswer failed - reason={}", e.getMessage());
			throw new BusinessException(ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);
		}

		if (ragAnswerRes == null || ragAnswerRes.isEmpty()) {
			emptyCounter.increment();
			log.warn("[AI Server] generateAnswer empty response");
			throw new EmptyAiResponseException();
		}

		successCounter.increment();
		return ragAnswerRes.get(0);
	}
}
