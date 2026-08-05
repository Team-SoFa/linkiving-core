package com.sofa.linkiving.domain.link.analytics;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sofa.linkiving.global.analytics.AnalyticsContext;
import com.sofa.linkiving.global.analytics.Ga4Event;
import com.sofa.linkiving.global.analytics.Ga4Publisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SummaryAnalyticsPublisher {

	private final Ga4Publisher ga4Publisher;

	public void publishComplete(
		AnalyticsContext analyticsContext,
		Long memberId,
		Long linkId,
		boolean isError,
		long startedAtNanos
	) {
		if (analyticsContext == null || analyticsContext.clientId() == null || analyticsContext.clientId().isBlank()) {
			return;
		}

		Map<String, Object> params = new HashMap<>();
		params.put("bookmark_id", linkId);
		params.put("is_error", isError);
		params.put("latency_ms", elapsedMillis(startedAtNanos));

		String userId = memberId == null ? null : String.valueOf(memberId);
		ga4Publisher.publish(analyticsContext.clientId(), userId, new Ga4Event("summary_complete", params));
	}

	private long elapsedMillis(long startedAtNanos) {
		return (System.nanoTime() - startedAtNanos) / 1_000_000;
	}
}
