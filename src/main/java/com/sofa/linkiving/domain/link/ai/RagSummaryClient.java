package com.sofa.linkiving.domain.link.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.RagInitialSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.RagRegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;

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
		try {
			RagInitialSummaryReq req = new RagInitialSummaryReq(linkId, userId, title, url, memo);
			List<RagInitialSummaryRes> response = ragSummaryFeign.requestInitialSummary(req);

			if (response != null && !response.isEmpty()) {
				log.info("[AI Server]  Initial Summary Requested Success. LinkId: {}", linkId);
				initialSuccess.increment();
				return response.get(0);
			}

			initialEmpty.increment();
			return null;

		} catch (Exception e) {
			log.error("[AI Server Error] Failed to request initial summary for LinkId: {}. Error: {}", linkId,
				e.getMessage());
			initialFailure.increment();
			return null;
		}
	}

	@Override
	public RagRegenerateSummaryRes regenerateSummary(Long linkId, Long userId, String url, String existingSummary) {
		try {
			RagRegenerateSummaryReq req = new RagRegenerateSummaryReq(linkId, userId, url, existingSummary);
			List<RagRegenerateSummaryRes> response = ragSummaryFeign.requestRegenerateSummary(req);

			if (response != null && !response.isEmpty()) {
				log.info("[AI Server] Regenerate Summary Success. LinkId: {}", linkId);
				regenerateSuccess.increment();
				return response.get(0);
			}

			regenerateEmpty.increment();
			return null;

		} catch (Exception e) {
			log.error("[AI Server Error] Failed to regenerate summary for LinkId: {}. Error: {}", linkId,
				e.getMessage());
			regenerateFailure.increment();
			return null;
		}
	}
}
