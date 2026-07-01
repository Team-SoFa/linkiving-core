package com.sofa.linkiving.domain.link.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.RagInitialSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.RagRegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;
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
public class RagSummaryClient implements SummaryClient {

	private static final String CLIENT = "summary";

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
		this.initialSuccess = buildCounter("initial", "success");
		this.initialEmpty = buildCounter("initial", "empty");
		this.initialFailure = buildCounter("initial", "failure");
		this.regenerateSuccess = buildCounter("regenerate", "success");
		this.regenerateEmpty = buildCounter("regenerate", "empty");
		this.regenerateFailure = buildCounter("regenerate", "failure");
	}

	private Counter buildCounter(String operation, String result) {
		return Counter.builder("ai.client.calls")
			.tag("client", "summary")
			.tag("operation", operation)
			.tag("result", result)
			.register(meterRegistry);
	}

	@Override
	public RagInitialSummaryRes initialSummary(Long linkId, Long userId, String title, String url, String memo) {
		String operation = "initialSummary";
		long startNanos = System.nanoTime();
		List<RagInitialSummaryRes> response;
		try {
			RagInitialSummaryReq req = new RagInitialSummaryReq(linkId, userId, title, url, memo);
			response = ragSummaryFeign.requestInitialSummary(req);
		} catch (Exception e) {
			throw ExternalApiSupport.handleFailure(CLIENT, operation, linkId, initialFailure, startNanos, e);
		}

		RagInitialSummaryRes result = firstOrThrowEmpty(response, operation, linkId, initialEmpty, startNanos);
		initialSuccess.increment();
		return result;
	}

	@Override
	public RagRegenerateSummaryRes regenerateSummary(Long linkId, Long userId, String url, String existingSummary) {
		String operation = "regenerateSummary";
		long startNanos = System.nanoTime();
		List<RagRegenerateSummaryRes> response;
		try {
			RagRegenerateSummaryReq req = new RagRegenerateSummaryReq(linkId, userId, url, existingSummary);
			response = ragSummaryFeign.requestRegenerateSummary(req);
		} catch (Exception e) {
			throw ExternalApiSupport.handleFailure(CLIENT, operation, linkId, regenerateFailure, startNanos, e);
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
		ExternalApiLogger.client(CLIENT, operation)
			.detail("linkId", linkId)
			.elapsedMs(ExternalApiSupport.elapsedMs(startNanos))
			.empty();
		throw new EmptyAiResponseException();
	}
}
