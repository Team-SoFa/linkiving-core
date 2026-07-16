package com.sofa.linkiving.domain.link.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.RagInitialSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.RagRegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RagSummaryClient implements SummaryClient {

	private static final Client CLIENT = Client.SUMMARY;

	private final RagSummaryFeign ragSummaryFeign;
	private final MeterRegistry meterRegistry;
	private Counter initialSuccess;
	private Counter initialEmpty;
	private Counter initialFailure;
	private Counter regenerateSuccess;
	private Counter regenerateEmpty;
	private Counter regenerateFailure;

	@PostConstruct
	private void initCounters() {
		this.initialSuccess = buildCounter(Operation.INITIAL, Result.SUCCESS);
		this.initialEmpty = buildCounter(Operation.INITIAL, Result.EMPTY);
		this.initialFailure = buildCounter(Operation.INITIAL, Result.FAILURE);
		this.regenerateSuccess = buildCounter(Operation.REGENERATE, Result.SUCCESS);
		this.regenerateEmpty = buildCounter(Operation.REGENERATE, Result.EMPTY);
		this.regenerateFailure = buildCounter(Operation.REGENERATE, Result.FAILURE);
	}

	private Counter buildCounter(Operation operation, Result result) {
		return AiClientMetrics.counter(meterRegistry, CLIENT, operation, result);
	}

	@Override
	public RagInitialSummaryRes initialSummary(Long linkId, Long userId, String title, String url, String memo) {
		String operation = Operation.INITIAL.getValue();
		long startNanos = System.nanoTime();
		List<RagInitialSummaryRes> response;
		try {
			RagInitialSummaryReq req = new RagInitialSummaryReq(linkId, userId, title, url, memo);
			response = ragSummaryFeign.requestInitialSummary(req);
		} catch (Exception e) {
			throw ExternalApiSupport.handleFailure(CLIENT.getValue(), operation, linkId, initialFailure, startNanos, e);
		}

		RagInitialSummaryRes result = firstOrThrowEmpty(response, operation, linkId, initialEmpty, startNanos);
		initialSuccess.increment();
		return result;
	}

	@Override
	public RagRegenerateSummaryRes regenerateSummary(Long linkId, Long userId, String url, String existingSummary) {
		String operation = Operation.REGENERATE.getValue();
		long startNanos = System.nanoTime();
		List<RagRegenerateSummaryRes> response;
		try {
			RagRegenerateSummaryReq req = new RagRegenerateSummaryReq(linkId, userId, url, existingSummary);
			response = ragSummaryFeign.requestRegenerateSummary(req);
		} catch (Exception e) {
			throw ExternalApiSupport.handleFailure(CLIENT.getValue(), operation, linkId, regenerateFailure,
				startNanos, e);
		}

		RagRegenerateSummaryRes result = firstOrThrowEmpty(response, operation, linkId, regenerateEmpty, startNanos);
		regenerateSuccess.increment();
		return result;
	}

	private <T> T firstOrThrowEmpty(List<T> response, String operation, Long linkId, Counter emptyCounter,
		long startNanos) {
		if (response != null && !response.isEmpty()) {
			return response.get(0);
		}
		emptyCounter.increment();
		ExternalApiLogger.client(CLIENT.getValue(), operation)
			.detail("linkId", linkId)
			.elapsedMs(ExternalApiSupport.elapsedMs(startNanos))
			.empty();
		throw new EmptyAiResponseException();
	}
}
