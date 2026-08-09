package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class PendingTermsMemberCleanupSchedulerTest {

	@Test
	void shouldDeleteExpiredPendingTermsMembers() {
		// given
		MemberRepository memberRepository = mock(MemberRepository.class);
		PendingTermsMemberCleanupScheduler scheduler =
			new PendingTermsMemberCleanupScheduler(memberRepository, 14, new SimpleMeterRegistry());
		ReflectionTestUtils.invokeMethod(scheduler, "initCounters");
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

	@Test
	void shouldCountFailureWhenCleanupFails() {
		// given
		MemberRepository memberRepository = mock(MemberRepository.class);
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		PendingTermsMemberCleanupScheduler scheduler =
			new PendingTermsMemberCleanupScheduler(memberRepository, 14, meterRegistry);
		ReflectionTestUtils.invokeMethod(scheduler, "initCounters");

		given(memberRepository.deleteByStatusAndTermsAgreedAtIsNullAndPrivacyAgreedAtIsNullAndCreatedAtBefore(
			any(), any())).willThrow(new RuntimeException("boom"));

		// when
		scheduler.deleteExpiredPendingTermsMembers();

		// then
		assertThat(meterRegistry.counter("async.task.failures",
			"task", "member", "action", "DELETE").count()).isEqualTo(1.0);
	}
}
