package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.global.logging.ExternalApiLogger;
import com.sofa.linkiving.global.metrics.AiClientMetrics;
import com.sofa.linkiving.global.metrics.AiClientMetrics.Client;
import com.sofa.linkiving.global.metrics.AiClientMetrics.Operation;
import com.sofa.linkiving.global.metrics.AiClientMetrics.Result;
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

	private static final Client CLIENT = Client.ANSWER;
	private static final Operation OPERATION = Operation.GENERATE;

	private final RagAnswerFeign ragAnswerFeign;
	private final MeterRegistry meterRegistry;

	private Counter successCounter;
	private Counter emptyCounter;
	private Counter failureCounter;

	@PostConstruct
	private void initCounters() {
		this.successCounter = buildCounter(Result.SUCCESS);
		this.emptyCounter = buildCounter(Result.EMPTY);
		this.failureCounter = buildCounter(Result.FAILURE);
	}

	private Counter buildCounter(Result result) {
		return AiClientMetrics.counter(meterRegistry, CLIENT, OPERATION, result);
	}

	@Override
	public RagAnswerRes generateAnswer(RagAnswerReq request) {
		long startNanos = System.nanoTime();
		List<RagAnswerRes> ragAnswerRes;
		try {
			ragAnswerRes = ragAnswerFeign.generateAnswer(request);
		} catch (Exception e) {
			throw ExternalApiSupport.handleFailure(CLIENT.getValue(), OPERATION.getValue(), null, failureCounter,
				startNanos, e);
		}

		if (ragAnswerRes == null || ragAnswerRes.isEmpty()) {
			emptyCounter.increment();
			ExternalApiLogger.client(CLIENT.getValue(), OPERATION.getValue())
				.elapsedMs(ExternalApiSupport.elapsedMs(startNanos))
				.empty();
			throw new EmptyAiResponseException();
		}

		successCounter.increment();
		return ragAnswerRes.get(0);
	}
}
