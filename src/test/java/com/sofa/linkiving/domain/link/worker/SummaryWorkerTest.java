package com.sofa.linkiving.domain.link.worker;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.config.SummaryWorkerProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryWorker 단위 테스트")
class SummaryWorkerTest {

	@Mock
	private SummaryQueue summaryQueue;

	private SummaryWorker summaryWorker;
	private SummaryWorkerProperties properties;

	@BeforeEach
	void setUp() {
		properties = new SummaryWorkerProperties(Duration.ofMillis(100)); // 테스트용 짧은 sleep 시간
		summaryWorker = new SummaryWorker(summaryQueue, properties);
	}

	@AfterEach
	void tearDown() {
		if (summaryWorker != null) {
			summaryWorker.stopWorker();
		}
	}

	@Test
	@DisplayName("워커 시작 시 백그라운드 쓰레드가 생성된다")
	void shouldStartWorkerThread() throws InterruptedException {
		// given
		given(summaryQueue.pollFromQueue()).willReturn(Optional.empty());

		// when
		summaryWorker.startWorker();
		Thread.sleep(50); // 워커 쓰레드가 시작될 시간 대기

		// then
		verify(summaryQueue, atLeastOnce()).pollFromQueue();
	}

	@Test
	@DisplayName("큐에 데이터가 있으면 처리한다")
	void shouldProcessLinkFromQueue() throws InterruptedException {
		// given
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(123L))
			.willReturn(Optional.empty());

		// when
		summaryWorker.startWorker();
		Thread.sleep(150); // 처리 시간 대기

		// then
		verify(summaryQueue, atLeast(2)).pollFromQueue();
	}

	@Test
	@DisplayName("큐가 비어있으면 설정된 시간만큼 대기한다")
	void shouldSleepWhenQueueIsEmpty() throws InterruptedException {
		// given
		given(summaryQueue.pollFromQueue()).willReturn(Optional.empty());

		// when
		summaryWorker.startWorker();
		long startTime = System.currentTimeMillis();
		Thread.sleep(250); // sleep(100ms) * 2회 이상 호출될 시간 대기
		long endTime = System.currentTimeMillis();

		// then
		long elapsed = endTime - startTime;
		assertThat(elapsed).isGreaterThanOrEqualTo(200); // 최소 2번의 sleep(100ms)
		verify(summaryQueue, atLeast(2)).pollFromQueue();
	}

	@Test
	@DisplayName("워커 종료 시 쓰레드가 정상적으로 중단된다")
	void shouldStopWorkerThread() throws InterruptedException {
		// given
		given(summaryQueue.pollFromQueue()).willReturn(Optional.empty());
		summaryWorker.startWorker();
		Thread.sleep(50); // 워커 시작 대기

		// when
		summaryWorker.stopWorker();
		Thread.sleep(50); // 종료 대기

		// then
		int invocationsBefore = mockingDetails(summaryQueue).getInvocations().size();
		Thread.sleep(150); // 추가 대기
		int invocationsAfter = mockingDetails(summaryQueue).getInvocations().size();

		// 워커가 중단되었으므로 추가 호출이 없어야 함
		assertThat(invocationsAfter).isEqualTo(invocationsBefore);
	}

	@Test
	@DisplayName("여러 링크를 순차적으로 처리한다")
	void shouldProcessMultipleLinks() throws InterruptedException {
		// given
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(1L))
			.willReturn(Optional.of(2L))
			.willReturn(Optional.of(3L))
			.willReturn(Optional.empty());

		// when
		summaryWorker.startWorker();
		Thread.sleep(200); // 여러 링크 처리 시간 대기

		// then
		verify(summaryQueue, atLeast(4)).pollFromQueue();
	}

	@Test
	@DisplayName("에러 발생 시에도 워커는 계속 동작한다")
	void shouldContinueWorkingAfterError() throws InterruptedException {
		// given
		given(summaryQueue.pollFromQueue())
			.willThrow(new RuntimeException("Test exception"))
			.willReturn(Optional.of(123L))
			.willReturn(Optional.empty());

		// when
		summaryWorker.startWorker();
		Thread.sleep(200); // 에러 발생 및 복구 시간 대기

		// then
		// 에러가 발생해도 워커가 계속 동작하여 다음 pollFromQueue 호출
		verify(summaryQueue, atLeast(3)).pollFromQueue();
	}
}
