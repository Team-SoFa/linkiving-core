package com.sofa.linkiving.domain.link.worker;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SummaryQueue 단위 테스트")
class SummaryQueueTest {

	private SummaryQueue summaryQueue;

	@BeforeEach
	void setUp() {
		summaryQueue = new SummaryQueue();
	}

	@Test
	@DisplayName("큐에 링크 ID를 추가할 수 있다")
	void shouldAddLinkIdToQueue() {
		// given
		Long linkId = 123L;

		// when
		summaryQueue.addToQueue(linkId);

		// then
		Optional<Long> result = summaryQueue.pollFromQueue();
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(123L);
	}

	@Test
	@DisplayName("큐에서 링크 ID를 꺼낼 수 있다")
	void shouldPollLinkIdFromQueue() {
		// given
		summaryQueue.addToQueue(456L);

		// when
		Optional<Long> result = summaryQueue.pollFromQueue();

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(456L);
	}

	@Test
	@DisplayName("빈 큐에서 poll 시 empty를 반환한다")
	void shouldReturnEmptyWhenQueueIsEmpty() {
		// when
		Optional<Long> result = summaryQueue.pollFromQueue();

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("여러 링크 ID를 FIFO 순서로 처리한다")
	void shouldProcessLinksInFifoOrder() {
		// given
		summaryQueue.addToQueue(1L);
		summaryQueue.addToQueue(2L);
		summaryQueue.addToQueue(3L);

		// when & then
		assertThat(summaryQueue.pollFromQueue().get()).isEqualTo(1L);
		assertThat(summaryQueue.pollFromQueue().get()).isEqualTo(2L);
		assertThat(summaryQueue.pollFromQueue().get()).isEqualTo(3L);
		assertThat(summaryQueue.pollFromQueue()).isEmpty();
	}

	@Test
	@DisplayName("동일한 링크 ID를 여러 번 추가할 수 있다")
	void shouldAddSameLinkIdMultipleTimes() {
		// given
		Long linkId = 999L;

		// when
		summaryQueue.addToQueue(linkId);
		summaryQueue.addToQueue(linkId);

		// then
		assertThat(summaryQueue.pollFromQueue().get()).isEqualTo(999L);
		assertThat(summaryQueue.pollFromQueue().get()).isEqualTo(999L);
		assertThat(summaryQueue.pollFromQueue()).isEmpty();
	}
}

