package com.sofa.linkiving.domain.link.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.RagInitialSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.RagRegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;
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
public class RagSummaryClient implements SummaryClient {

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
		List<RagInitialSummaryRes> response;
		try {
			RagInitialSummaryReq req = new RagInitialSummaryReq(linkId, userId, title, url, memo);
			response = ragSummaryFeign.requestInitialSummary(req);
		} catch (BusinessException e) {
			initialFailure.increment();
			log.warn("[AI Server] Initial summary failed - linkId={}, code={}", linkId, e.getErrorCode().getCode());
			throw e;
		} catch (Exception e) {
			initialFailure.increment();
			log.warn("[AI Server] Initial summary failed - linkId={}, reason={}", linkId, e.getMessage());
			throw new BusinessException(ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);
		}

		if (response == null || response.isEmpty()) {
			initialEmpty.increment();
			log.warn("[AI Server] Initial summary empty response - linkId={}", linkId);
			throw new EmptyAiResponseException();
		}

		initialSuccess.increment();
		return response.get(0);
	}

	@Override
	public RagRegenerateSummaryRes regenerateSummary(Long linkId, Long userId, String url, String existingSummary) {
		List<RagRegenerateSummaryRes> response;
		try {
			RagRegenerateSummaryReq req = new RagRegenerateSummaryReq(linkId, userId, url, existingSummary);
			response = ragSummaryFeign.requestRegenerateSummary(req);
		} catch (BusinessException e) {
			regenerateFailure.increment();
			log.warn("[AI Server] Regenerate summary failed - linkId={}, code={}", linkId, e.getErrorCode().getCode());
			throw e;
		} catch (Exception e) {
			regenerateFailure.increment();
			log.warn("[AI Server] Regenerate summary failed - linkId={}, reason={}", linkId, e.getMessage());
			throw new BusinessException(ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);
		}

		if (response == null || response.isEmpty()) {
			regenerateEmpty.increment();
			log.warn("[AI Server] Regenerate summary empty response - linkId={}", linkId);
			throw new EmptyAiResponseException();
		}

		regenerateSuccess.increment();
		return response.get(0);
	}
}
