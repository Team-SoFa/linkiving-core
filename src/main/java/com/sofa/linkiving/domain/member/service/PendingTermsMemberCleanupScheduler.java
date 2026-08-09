package com.sofa.linkiving.domain.member.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.global.logging.AuditLogger;
import com.sofa.linkiving.global.metrics.AsyncTaskMetrics;
import com.sofa.linkiving.global.metrics.AsyncTaskMetrics.Action;
import com.sofa.linkiving.global.metrics.AsyncTaskMetrics.Task;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PendingTermsMemberCleanupScheduler {
	private final MemberRepository memberRepository;
	private final int retentionDays;
	private final MeterRegistry meterRegistry;
	private Counter cleanupFailureCounter;

	public PendingTermsMemberCleanupScheduler(
		MemberRepository memberRepository,
		@Value("${app.member.pending-terms-retention-days:14}") int retentionDays,
		MeterRegistry meterRegistry
	) {
		this.memberRepository = memberRepository;
		this.retentionDays = retentionDays;
		this.meterRegistry = meterRegistry;
	}

	@PostConstruct
	private void initCounters() {
		this.cleanupFailureCounter = AsyncTaskMetrics.failureCounter(meterRegistry, Task.MEMBER, Action.DELETE);
	}

	@Scheduled(cron = "${app.member.pending-terms-cleanup-cron:0 0 4 * * *}")
	@Transactional
	public void deleteExpiredPendingTermsMembers() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
		try {
			long deletedCount = memberRepository
				.deleteByStatusAndTermsAgreedAtIsNullAndPrivacyAgreedAtIsNullAndCreatedAtBefore(
					MemberStatus.PENDING_TERMS,
					cutoff
				);

			if (deletedCount > 0) {
				AuditLogger.info("event=pending_terms_cleanup result=SUCCESS deleted={} cutoff={}",
					deletedCount, cutoff);
			}
		} catch (Exception e) {
			cleanupFailureCounter.increment();
			log.error("Failed to cleanup expired pending terms members - cutoff: {}", cutoff, e);
		}
	}
}
