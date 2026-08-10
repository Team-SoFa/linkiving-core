package com.sofa.linkiving.domain.member.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.worker.SummaryQueue;
import com.sofa.linkiving.domain.member.ai.MemberVectorClient;
import com.sofa.linkiving.domain.member.config.MemberWithdrawalProperties;
import com.sofa.linkiving.domain.member.enums.MemberDeleteReason;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.analytics.Ga4Event;
import com.sofa.linkiving.global.analytics.Ga4Publisher;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.global.metrics.MemberWithdrawalMetrics;
import com.sofa.linkiving.infra.redis.RedisKeyRegistry;
import com.sofa.linkiving.infra.redis.RedisService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberWithdrawalService {

	private final MemberVectorClient memberVectorClient;
	private final MemberDataDeletionService memberDataDeletionService;
	private final RedisService redisService;
	private final SummaryQueue summaryQueue;
	private final Ga4Publisher ga4Publisher;
	private final MemberWithdrawalProperties properties;
	private final MeterRegistry meterRegistry;
	private Counter vectorDeletionFailureCounter;
	private Counter refreshTokenDeletionFailureCounter;

	@PostConstruct
	private void initCounters() {
		this.vectorDeletionFailureCounter = MemberWithdrawalMetrics.vectorDeletionFailureCounter(meterRegistry);
		this.refreshTokenDeletionFailureCounter =
			MemberWithdrawalMetrics.refreshTokenDeletionFailureCounter(meterRegistry);
	}

	public void withdraw(Long memberId, String email, String clientId, MemberDeleteReason deleteReason) {
		if (!properties.enabled()) {
			throw new BusinessException(MemberErrorCode.WITHDRAWAL_DISABLED);
		}
		try {
			memberDataDeletionService.beginWithdrawal(memberId, email);
			deleteRefreshTokenBestEffort(memberId, email);
			summaryQueue.removeForMember(memberId);

			deleteMemberVectorsBestEffort(memberId);
			if (memberDataDeletionService.claimWithdrawalAnalytics(memberId, email)) {
				ga4Publisher.publishBestEffort(clientId, String.valueOf(memberId),
					new Ga4Event("account_delete", Map.of("delete_reason", deleteReason.name())));
			}
			memberDataDeletionService.deleteAll(memberId, email);
		} catch (BusinessException exception) {
			if (exception.getErrorCode() != MemberErrorCode.USER_NOT_FOUND) {
				throw exception;
			}
			log.info("이미 완료된 회원 탈퇴 요청을 멱등 성공으로 처리합니다. memberId={}", memberId);
		}
	}

	private void deleteRefreshTokenBestEffort(Long memberId, String email) {
		try {
			redisService.delete(RedisKeyRegistry.REFRESH_TOKEN, email);
		} catch (RuntimeException exception) {
			refreshTokenDeletionFailureCounter.increment();
			log.error("회원 탈퇴 중 refresh token 삭제에 실패했습니다. memberId={}, cause={}",
				memberId, exception.getClass().getSimpleName());
		}
	}

	private void deleteMemberVectorsBestEffort(Long memberId) {
		try {
			memberVectorClient.validateConfiguration();
			memberVectorClient.deleteAll(memberId);
		} catch (RuntimeException exception) {
			vectorDeletionFailureCounter.increment();
			log.error("회원 탈퇴 중 AI 벡터 삭제에 실패했습니다. memberId={}, cause={}",
				memberId, exception.getClass().getSimpleName());
		}
	}
}
