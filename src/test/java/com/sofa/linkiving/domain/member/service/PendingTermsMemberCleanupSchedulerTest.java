package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

class PendingTermsMemberCleanupSchedulerTest {

	@Test
	void shouldDeleteExpiredPendingTermsMembers() {
		// given
		MemberRepository memberRepository = mock(MemberRepository.class);
		PendingTermsMemberCleanupScheduler scheduler = new PendingTermsMemberCleanupScheduler(memberRepository, 14);
		ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		LocalDateTime before = LocalDateTime.now().minusDays(14);

		// when
		scheduler.deleteExpiredPendingTermsMembers();

		// then
		LocalDateTime after = LocalDateTime.now().minusDays(14);
		verify(memberRepository, times(1))
			.deleteByStatusAndTermsAgreedAtIsNullAndPrivacyAgreedAtIsNullAndCreatedAtBefore(
				eq(MemberStatus.PENDING_TERMS),
				cutoffCaptor.capture()
			);
		assertThat(cutoffCaptor.getValue()).isBetween(before, after);
	}
}
