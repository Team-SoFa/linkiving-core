package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.link.worker.SummaryQueue;
import com.sofa.linkiving.domain.member.ai.MemberVectorClient;
import com.sofa.linkiving.domain.member.config.MemberWithdrawalProperties;
import com.sofa.linkiving.domain.member.enums.MemberDeleteReason;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.analytics.Ga4Event;
import com.sofa.linkiving.global.analytics.Ga4Publisher;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.infra.redis.RedisKeyRegistry;
import com.sofa.linkiving.infra.redis.RedisService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawalServiceTest {

	@Mock MemberVectorClient memberVectorClient;
	@Mock MemberDataDeletionService memberDataDeletionService;
	@Mock RedisService redisService;
	@Mock SummaryQueue summaryQueue;
	@Mock Ga4Publisher ga4Publisher;
	private MemberWithdrawalService memberWithdrawalService;
	private SimpleMeterRegistry meterRegistry;

	@BeforeEach
	void setUp() {
		meterRegistry = new SimpleMeterRegistry();
		memberWithdrawalService = new MemberWithdrawalService(memberVectorClient, memberDataDeletionService,
			redisService, summaryQueue, ga4Publisher,
			new MemberWithdrawalProperties(true, "secret", Duration.ofMinutes(10)), meterRegistry);
		ReflectionTestUtils.invokeMethod(memberWithdrawalService, "initCounters");
	}

	@Test
	void shouldBlockRefreshAndQueueBeforeExternalAndDatabaseDeletion() {
		org.mockito.BDDMockito.given(memberDataDeletionService.claimWithdrawalAnalytics(1L, "member@test.com"))
			.willReturn(true);
		memberWithdrawalService.withdraw(1L, "member@test.com", "123.456", MemberDeleteReason.OTHER);

		InOrder order = inOrder(memberDataDeletionService, redisService, summaryQueue,
			memberVectorClient, ga4Publisher);
		order.verify(memberDataDeletionService).beginWithdrawal(1L, "member@test.com");
		order.verify(redisService).delete(RedisKeyRegistry.REFRESH_TOKEN, "member@test.com");
		order.verify(summaryQueue).removeForMember(1L);
		order.verify(memberVectorClient).validateConfiguration();
		order.verify(memberVectorClient).deleteAll(1L);
		order.verify(memberDataDeletionService).claimWithdrawalAnalytics(1L, "member@test.com");
		order.verify(ga4Publisher).publishBestEffort("123.456", "1",
			new Ga4Event("account_delete", java.util.Map.of("delete_reason", "OTHER")));
		order.verify(memberDataDeletionService).deleteAll(1L, "member@test.com");
	}

	@Test
	void shouldCompleteWithdrawalAndRecordMetricWhenVectorDeletionFails() {
		willThrow(new IllegalStateException("qdrant unavailable")).given(memberVectorClient).deleteAll(1L);
		org.mockito.BDDMockito.given(memberDataDeletionService.claimWithdrawalAnalytics(1L, "member@test.com"))
			.willReturn(true);

		memberWithdrawalService.withdraw(1L, "member@test.com", "123.456", MemberDeleteReason.OTHER);

		verify(memberDataDeletionService).deleteAll(1L, "member@test.com");
		verify(ga4Publisher).publishBestEffort("123.456", "1",
			new Ga4Event("account_delete", java.util.Map.of("delete_reason", "OTHER")));
		assertThat(meterRegistry.get("member.withdrawal.vector.deletion.failures").counter().count())
			.isEqualTo(1.0);
	}

	@Test
	void shouldCompleteWithdrawalWhenVectorDeletionConfigurationIsInvalid() {
		willThrow(new IllegalStateException("internal secret missing"))
			.given(memberVectorClient).validateConfiguration();

		memberWithdrawalService.withdraw(1L, "member@test.com", "123.456", MemberDeleteReason.OTHER);

		verify(memberVectorClient, never()).deleteAll(1L);
		verify(memberDataDeletionService).deleteAll(1L, "member@test.com");
		assertThat(meterRegistry.get("member.withdrawal.vector.deletion.failures").counter().count())
			.isEqualTo(1.0);
	}

	@Test
	void shouldCompleteWithdrawalAndRecordMetricWhenRefreshTokenDeletionFails() {
		willThrow(new IllegalStateException("redis unavailable")).given(redisService)
			.delete(RedisKeyRegistry.REFRESH_TOKEN, "member@test.com");

		memberWithdrawalService.withdraw(1L, "member@test.com", "123.456", MemberDeleteReason.OTHER);

		verify(summaryQueue).removeForMember(1L);
		verify(memberVectorClient).deleteAll(1L);
		verify(memberDataDeletionService).deleteAll(1L, "member@test.com");
		assertThat(meterRegistry.get("member.withdrawal.refresh.token.deletion.failures").counter().count())
			.isEqualTo(1.0);
	}

	@Test
	void shouldKeepWithdrawingStateForRetryWhenDatabaseDeletionFails() {
		org.mockito.BDDMockito.given(memberDataDeletionService.claimWithdrawalAnalytics(1L, "member@test.com"))
			.willReturn(true);
		willThrow(new IllegalStateException("db failure")).given(memberDataDeletionService)
			.deleteAll(1L, "member@test.com");

		assertThatThrownBy(() -> memberWithdrawalService.withdraw(
			1L, "member@test.com", "123.456", MemberDeleteReason.OTHER))
			.isInstanceOf(IllegalStateException.class);

		verify(memberVectorClient).deleteAll(1L);
		verify(memberDataDeletionService).beginWithdrawal(1L, "member@test.com");
	}

	@Test
	void shouldNotPublishAccountDeleteTwiceOnRetry() {
		memberWithdrawalService.withdraw(1L, "member@test.com", "123.456", MemberDeleteReason.OTHER);

		verify(memberDataDeletionService).claimWithdrawalAnalytics(1L, "member@test.com");
		verify(ga4Publisher, never()).publishBestEffort(
			org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any());
	}

	@Test
	void shouldTreatAlreadyDeletedMemberAsIdempotentSuccessAtStart() {
		willThrow(new BusinessException(MemberErrorCode.USER_NOT_FOUND))
			.given(memberDataDeletionService).beginWithdrawal(1L, "member@test.com");

		assertThatCode(() -> memberWithdrawalService.withdraw(
			1L, "member@test.com", "123.456", MemberDeleteReason.OTHER))
			.doesNotThrowAnyException();

		verify(redisService, never()).delete(RedisKeyRegistry.REFRESH_TOKEN, "member@test.com");
	}

	@Test
	void shouldTreatConcurrentDeletionAsIdempotentSuccessAtFinalDelete() {
		willThrow(new BusinessException(MemberErrorCode.USER_NOT_FOUND))
			.given(memberDataDeletionService).deleteAll(1L, "member@test.com");

		assertThatCode(() -> memberWithdrawalService.withdraw(
			1L, "member@test.com", "123.456", MemberDeleteReason.OTHER))
			.doesNotThrowAnyException();
	}
}
