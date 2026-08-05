package com.sofa.linkiving.domain.member.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PendingTermsMemberCleanupScheduler {
	private final MemberRepository memberRepository;
	private final int retentionDays;

	public PendingTermsMemberCleanupScheduler(
		MemberRepository memberRepository,
		@Value("${app.member.pending-terms-retention-days:14}") int retentionDays
	) {
		this.memberRepository = memberRepository;
		this.retentionDays = retentionDays;
	}

	@Scheduled(cron = "${app.member.pending-terms-cleanup-cron:0 0 4 * * *}")
	@Transactional
	public void deleteExpiredPendingTermsMembers() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
		long deletedCount = memberRepository
			.deleteByStatusAndTermsAgreedAtIsNullAndPrivacyAgreedAtIsNullAndCreatedAtBefore(
				MemberStatus.PENDING_TERMS,
				cutoff
			);

		if (deletedCount > 0) {
			log.info("Deleted expired pending terms members - count: {}, cutoff: {}", deletedCount, cutoff);
		}
	}
}
