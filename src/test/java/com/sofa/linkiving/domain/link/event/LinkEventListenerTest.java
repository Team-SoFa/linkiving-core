package com.sofa.linkiving.domain.link.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.facade.SummaryWorkerFacade;
import com.sofa.linkiving.domain.link.worker.SummaryQueue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LinkEventListenerTest.RetryTestConfig.class)
@DisplayName("LinkEventListener 재시도(Retry) 및 복구(Recover) 단위 테스트")
class LinkEventListenerTest {

	@Autowired
	private ApplicationEventPublisher eventPublisher;
	@Autowired
	private SummaryWorkerFacade summaryWorkerFacade;
	@Autowired
	private LinkEventListener linkEventListener;
	@Autowired
	private SummaryQueue summaryQueue;

	@BeforeEach
	void setUp() {
		reset(summaryQueue, eventPublisher, summaryWorkerFacade);
	}

	@Test
	@DisplayName("링크 생성 이벤트 수신 시 큐에 추가한다 & 첫 번째 시도에서 성공하면 재시도하지 않는다")
	void shouldAddQueueAndNotRetry_WhenFirstAttemptSucceeds() {
		// given
		LinkCreatedEvent event = new LinkCreatedEvent(1L, "test@test.com");
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
		LinkCreatedEvent event1 = new LinkCreatedEvent(10L, "test1@test.com");
		LinkCreatedEvent event2 = new LinkCreatedEvent(20L, "test2@test.com");
		LinkCreatedEvent event3 = new LinkCreatedEvent(30L, "test3@test.com");
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
		LinkCreatedEvent event = new LinkCreatedEvent(1L, "test1@test.com");

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
		LinkCreatedEvent event = new LinkCreatedEvent(2L, "test2@test.com");

		doThrow(new RuntimeException("Queue full")).when(summaryQueue).addToQueue(anyLong());

		// when
		linkEventListener.handleLinkCreated(event);

		// then
		verify(summaryQueue, times(3)).addToQueue(2L);
	}

	@Test
	@DisplayName("3회 재시도 모두 실패 시 Recover 로직을 타며 DB를 FAILED로 변경하고 이벤트를 발행함")
	void shouldPublishFailedEvent_WhenAllRetriesFail() {
		// given
		LinkCreatedEvent event = new LinkCreatedEvent(2L, "fail@test.com");
		doThrow(new RuntimeException("Queue Full")).when(summaryQueue).addToQueue(anyLong());

		// when
		linkEventListener.handleLinkCreated(event);

		// then
		// 메서드 시작 시 PENDING 1번, recover 내부에서 FAILED 1번으로 총 2번 발행되어야 함
		ArgumentCaptor<SummaryStatusEvent> captor = ArgumentCaptor.forClass(SummaryStatusEvent.class);
		verify(eventPublisher, times(2)).publishEvent(captor.capture());

		List<SummaryStatusEvent> publishedEvents = captor.getAllValues();

		// 첫 번째 이벤트 (PENDING)
		assertThat(publishedEvents.get(0).email()).isEqualTo("fail@test.com");
		assertThat(publishedEvents.get(0).response().status()).isEqualTo(SummaryStatus.PENDING);

		// 두 번째 이벤트 (FAILED)
		assertThat(publishedEvents.get(1).email()).isEqualTo("fail@test.com");
		assertThat(publishedEvents.get(1).response().status()).isEqualTo(SummaryStatus.FAILED);
		assertThat(publishedEvents.get(1).response().data()).isEqualTo("요약 대기열 등록에 실패했습니다.");

		// Facade를 통한 DB 상태 업데이트가 호출되었는지 검증
		verify(summaryWorkerFacade, times(1)).updateSummaryStatus(2L, SummaryStatus.FAILED);

		// 큐 적재 로직이 3회 시도되었는지 검증
		verify(summaryQueue, times(3)).addToQueue(2L);
	}

	@Test
	@DisplayName("큐 등록 성공 시 PENDING 상태 이벤트를 발행함")
	void shouldPublishPendingEvent_WhenQueueSucceeds() {
		// given
		LinkCreatedEvent event = new LinkCreatedEvent(1L, "test@test.com");
		doNothing().when(summaryQueue).addToQueue(anyLong());

		// when
		linkEventListener.handleLinkCreated(event);

		// then
		ArgumentCaptor<SummaryStatusEvent> captor = ArgumentCaptor.forClass(SummaryStatusEvent.class);
		verify(eventPublisher, times(1)).publishEvent(captor.capture());

		SummaryStatusEvent publishedEvent = captor.getValue();
		assertThat(publishedEvent.email()).isEqualTo("test@test.com");
		assertThat(publishedEvent.response().status()).isEqualTo(SummaryStatus.PENDING);
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
		public ApplicationEventPublisher eventPublisher() {
			return mock(ApplicationEventPublisher.class);
		}

		@Bean
		public SummaryWorkerFacade summaryWorkerFacade() {
			return mock(SummaryWorkerFacade.class);
		}

		@Bean
		public LinkEventListener linkEventListener(SummaryQueue summaryQueue,
			ApplicationEventPublisher eventPublisher,
			SummaryWorkerFacade summaryWorkerFacade,
			ObjectProvider<LinkEventListener> selfProvider) {
			return new LinkEventListener(summaryQueue, eventPublisher, summaryWorkerFacade, selfProvider);
		}
	}
}
