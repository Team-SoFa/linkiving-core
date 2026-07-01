package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.global.logging.ExternalApiLogger;
import com.sofa.linkiving.infra.feign.EmptyAiResponseException;
import com.sofa.linkiving.infra.feign.ExternalApiSupport;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RagAnswerClient implements AnswerClient {

	private static final String CLIENT = "answer";
	private static final String OPERATION = "generateAnswer";

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
		long startNanos = System.nanoTime();
		List<RagAnswerRes> ragAnswerRes;
		try {
			ragAnswerRes = ragAnswerFeign.generateAnswer(request);
		} catch (Exception e) {
			throw ExternalApiSupport.handleFailure(CLIENT, OPERATION, null, failureCounter, startNanos, e);
		}

		if (ragAnswerRes == null || ragAnswerRes.isEmpty()) {
			emptyCounter.increment();
			ExternalApiLogger.client(CLIENT, OPERATION)
				.elapsedMs(ExternalApiSupport.elapsedMs(startNanos))
				.empty();
			throw new EmptyAiResponseException();
		}

		successCounter.increment();
		return ragAnswerRes.get(0);
	}
}
