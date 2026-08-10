package com.sofa.linkiving.domain.member.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.global.logging.AuditLogger;
import com.sofa.linkiving.global.metrics.MemberWithdrawalMetrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MemberWithdrawalCleanupScheduler {

	private static final List<MemberStatus> STUCK_STATUSES = List.of(
		MemberStatus.WITHDRAWING,
		MemberStatus.WITHDRAWAL_ANALYTICS_SENT
	);

	private final MemberRepository memberRepository;
	private final MemberDataDeletionService memberDataDeletionService;
	private final MeterRegistry meterRegistry;
	private final Duration staleAfter;
	private Counter cleanupFailureCounter;

	public MemberWithdrawalCleanupScheduler(
		MemberRepository memberRepository,
		MemberDataDeletionService memberDataDeletionService,
		MeterRegistry meterRegistry,
		@Value("${app.member.withdrawal-cleanup-stale-after:PT1H}") Duration staleAfter
	) {
		this.memberRepository = memberRepository;
		this.memberDataDeletionService = memberDataDeletionService;
		this.meterRegistry = meterRegistry;
		this.staleAfter = staleAfter;
	}

	@PostConstruct
	private void initCounter() {
		this.cleanupFailureCounter = MemberWithdrawalMetrics.cleanupFailureCounter(meterRegistry);
	}

	@Scheduled(cron = "${app.member.withdrawal-cleanup-cron:0 30 4 * * *}")
	public void completeStuckWithdrawals() {
		List<Member> members;
		try {
			LocalDateTime cutoff = LocalDateTime.now().minus(staleAfter);
			members = memberRepository.findAllByStatusInAndUpdatedAtBefore(STUCK_STATUSES, cutoff);
		} catch (Exception exception) {
			recordFailure(null, exception);
			return;
		}

		for (Member member : members) {
			completeWithdrawal(member);
		}
	}

	private void completeWithdrawal(Member member) {
		try {
			memberDataDeletionService.deleteAll(member.getId(), member.getEmail());
			AuditLogger.info("event=withdrawal_cleanup result=SUCCESS memberId={}", member.getId());
		} catch (BusinessException exception) {
			if (exception.getErrorCode() == MemberErrorCode.USER_NOT_FOUND) {
				log.info("이미 삭제된 회원의 탈퇴 정리를 건너뜁니다. memberId={}", member.getId());
				return;
			}
			recordFailure(member.getId(), exception);
		} catch (Exception exception) {
			recordFailure(member.getId(), exception);
		}
	}

	private void recordFailure(Long memberId, Exception exception) {
		cleanupFailureCounter.increment();
		log.error("Failed to complete stuck withdrawal - memberId={}", memberId, exception);
	}
}
