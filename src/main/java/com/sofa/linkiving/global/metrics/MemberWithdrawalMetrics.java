package com.sofa.linkiving.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public final class MemberWithdrawalMetrics {

	private static final String VECTOR_DELETION_FAILURE_METRIC_NAME =
		"member.withdrawal.vector.deletion.failures";
	private static final String REFRESH_TOKEN_DELETION_FAILURE_METRIC_NAME =
		"member.withdrawal.refresh.token.deletion.failures";
	private static final String CLEANUP_FAILURE_METRIC_NAME =
		"member.withdrawal.cleanup.failures";

	private MemberWithdrawalMetrics() {
	}

	public static Counter vectorDeletionFailureCounter(MeterRegistry registry) {
		return Counter.builder(VECTOR_DELETION_FAILURE_METRIC_NAME)
			.description("회원 탈퇴 중 AI 벡터 삭제 실패 횟수")
			.register(registry);
	}

	public static Counter refreshTokenDeletionFailureCounter(MeterRegistry registry) {
		return Counter.builder(REFRESH_TOKEN_DELETION_FAILURE_METRIC_NAME)
			.description("회원 탈퇴 중 Redis refresh token 삭제 실패 횟수")
			.register(registry);
	}

	public static Counter cleanupFailureCounter(MeterRegistry registry) {
		return Counter.builder(CLEANUP_FAILURE_METRIC_NAME)
			.description("고착된 회원 탈퇴 데이터 정리 실패 횟수")
			.register(registry);
	}
}
