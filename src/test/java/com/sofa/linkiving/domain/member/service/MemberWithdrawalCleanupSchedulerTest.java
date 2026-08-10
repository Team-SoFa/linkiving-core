package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawalCleanupSchedulerTest {

	@Mock MemberRepository memberRepository;
	@Mock MemberDataDeletionService memberDataDeletionService;
	private SimpleMeterRegistry meterRegistry;
	private MemberWithdrawalCleanupScheduler scheduler;

	@BeforeEach
	void setUp() {
		meterRegistry = new SimpleMeterRegistry();
		scheduler = new MemberWithdrawalCleanupScheduler(
			memberRepository, memberDataDeletionService, meterRegistry, Duration.ofHours(1));
		ReflectionTestUtils.invokeMethod(scheduler, "initCounter");
	}

	@Test
	void shouldCompleteMembersLeftInWithdrawalStatuses() {
		Member withdrawing = member(1L, MemberStatus.WITHDRAWING);
		Member analyticsSent = member(2L, MemberStatus.WITHDRAWAL_ANALYTICS_SENT);
		given(memberRepository.findAllByStatusInAndUpdatedAtBefore(org.mockito.ArgumentMatchers.eq(List.of(
			MemberStatus.WITHDRAWING, MemberStatus.WITHDRAWAL_ANALYTICS_SENT)),
			org.mockito.ArgumentMatchers.any()))
			.willReturn(List.of(withdrawing, analyticsSent));

		scheduler.completeStuckWithdrawals();

		verify(memberDataDeletionService).deleteAll(1L, withdrawing.getEmail());
		verify(memberDataDeletionService).deleteAll(2L, analyticsSent.getEmail());
	}

	@Test
	void shouldRecordFailureAndContinueWhenCleanupFails() {
		Member first = member(1L, MemberStatus.WITHDRAWING);
		Member second = member(2L, MemberStatus.WITHDRAWING);
		given(memberRepository.findAllByStatusInAndUpdatedAtBefore(
			org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
			.willReturn(List.of(first, second));
		willThrow(new IllegalStateException("db failure"))
			.given(memberDataDeletionService).deleteAll(1L, first.getEmail());

		scheduler.completeStuckWithdrawals();

		verify(memberDataDeletionService).deleteAll(2L, second.getEmail());
		assertThat(meterRegistry.get("member.withdrawal.cleanup.failures").counter().count())
			.isEqualTo(1.0);
	}

	@Test
	void shouldIgnoreAlreadyDeletedMemberWithoutFailureMetric() {
		Member member = member(1L, MemberStatus.WITHDRAWING);
		given(memberRepository.findAllByStatusInAndUpdatedAtBefore(
			org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
			.willReturn(List.of(member));
		willThrow(new BusinessException(MemberErrorCode.USER_NOT_FOUND))
			.given(memberDataDeletionService).deleteAll(1L, member.getEmail());

		scheduler.completeStuckWithdrawals();

		assertThat(meterRegistry.get("member.withdrawal.cleanup.failures").counter().count())
			.isZero();
	}

	private Member member(Long id, MemberStatus status) {
		Member member = Member.builder()
			.email("member" + id + "@test.com")
			.password("password")
			.status(status)
			.build();
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}
}
