package com.sofa.linkiving.domain.link.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sofa.linkiving.domain.link.worker.SummaryQueue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LinkEventListenerTest.RetryTestConfig.class)
@DisplayName("LinkEventListener 재시도(Retry) 및 복구(Recover) 단위 테스트")
class LinkEventListenerTest {

	@Autowired
	private LinkEventListener linkEventListener;
	@Autowired
	private SummaryQueue summaryQueue;

	@BeforeEach
	void setUp() {
		reset(summaryQueue);
	}

	@Test
	@DisplayName("링크 생성 이벤트 수신 시 큐에 추가한다 & 첫 번째 시도에서 성공하면 재시도하지 않는다")
	void shouldAddQueueAndNotRetry_WhenFirstAttemptSucceeds() {
		// given
		LinkCreatedEvent event = new LinkCreatedEvent(1L);
		doNothing().when(summaryQueue).addToQueue(anyLong());

		// when
		linkEventListener.handleLinkCreated(event);

		// then
		verify(summaryQueue, times(1)).addToQueue(1L);
	}

	@Test
	@DisplayName("여러 링크 생성 이벤트를 순차적으로 처리한다")
	void shouldProcessMultipleEventsSequentially() {
		// given
		LinkCreatedEvent event1 = new LinkCreatedEvent(10L);
		LinkCreatedEvent event2 = new LinkCreatedEvent(20L);
		LinkCreatedEvent event3 = new LinkCreatedEvent(30L);
		doNothing().when(summaryQueue).addToQueue(anyLong());

		// when
		linkEventListener.handleLinkCreated(event1);
		linkEventListener.handleLinkCreated(event2);
		linkEventListener.handleLinkCreated(event3);

		// then
		verify(summaryQueue, times(1)).addToQueue(10L);
		verify(summaryQueue, times(1)).addToQueue(20L);
		verify(summaryQueue, times(1)).addToQueue(30L);
	}

	@Test
	@DisplayName("3번 내에 성공하면 오류가 발생하지 않는다 (2번 실패 후 3번째 성공)")
	void shouldNotThrowError_WhenSucceedsWithin3Times() {
		// given
		LinkCreatedEvent event = new LinkCreatedEvent(1L);

		doThrow(new RuntimeException("Queue full"))
			.doThrow(new RuntimeException("Queue full"))
			.doNothing()
			.when(summaryQueue).addToQueue(anyLong());

		// when & then
		assertThatCode(() -> linkEventListener.handleLinkCreated(event))
			.doesNotThrowAnyException();

		verify(summaryQueue, times(3)).addToQueue(1L);
	}

	@Test
	@DisplayName("큐 적재 실패 시 최대 3번까지 재시도")
	void shouldRetryUpTo3Times_WhenFails() {
		// given
		LinkCreatedEvent event = new LinkCreatedEvent(2L);

		doThrow(new RuntimeException("Queue full")).when(summaryQueue).addToQueue(anyLong());

		// when
		linkEventListener.handleLinkCreated(event);

		// then
		verify(summaryQueue, times(3)).addToQueue(2L);
	}

	@Test
	@DisplayName("최대 3번 한 후에 모두 실패하면 최종 오류(Recover) 로직 처리")
	void shouldExecuteRecoverLogic_WhenAll3RetriesFail() {
		// given
		LinkCreatedEvent event = new LinkCreatedEvent(3L);
		doThrow(new RuntimeException("Queue full")).when(summaryQueue).addToQueue(anyLong());

		// when & then
		assertThatCode(() -> linkEventListener.handleLinkCreated(event))
			.doesNotThrowAnyException();

		// 최종적으로 3번 호출된 것 검증
		verify(summaryQueue, times(3)).addToQueue(3L);
	}

	@Configuration
	@EnableRetry
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class RetryTestConfig {
		@Bean
		public SummaryQueue summaryQueue() {
			return mock(SummaryQueue.class);
		}

		@Bean
		public LinkEventListener linkEventListener(SummaryQueue summaryQueue) {
			return new LinkEventListener(summaryQueue);
		}
	}
}
