package com.sofa.linkiving.domain.link.worker;

import java.util.Map;

import com.sofa.linkiving.global.analytics.AnalyticsContext;

public record SummaryTask(
	Long linkId,
	Long memberId,
	AnalyticsContext analyticsContext,
	long startedAtNanos,
	Map<String, String> logContext
) {
}
