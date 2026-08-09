package com.sofa.linkiving.domain.link.event;

import java.util.Map;

import com.sofa.linkiving.global.analytics.AnalyticsContext;

/**
 * 링크 생성 완료 이벤트
 * 트랜잭션 커밋 이후 발행되는 이벤트
 */
public record LinkCreatedEvent(
	Long linkId,
	Long memberId,
	String email,
	Map<String, String> logContext,
	AnalyticsContext analyticsContext,
	long summaryStartedAtNanos
) {
	public LinkCreatedEvent(Long linkId, String email) {
		this(linkId, null, email, Map.of(), AnalyticsContext.of(null, null), System.nanoTime());
	}

	public LinkCreatedEvent(Long linkId, Long memberId, String email, Map<String, String> logContext) {
		this(linkId, memberId, email, logContext, AnalyticsContext.of(null, null), System.nanoTime());
	}
}
