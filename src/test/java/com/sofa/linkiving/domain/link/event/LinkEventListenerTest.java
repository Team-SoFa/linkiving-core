package com.sofa.linkiving.domain.link.event;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.worker.SummaryQueue;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkEventListener 단위 테스트")
class LinkEventListenerTest {

	@InjectMocks
	private LinkEventListener linkEventListener;

	@Mock
	private SummaryQueue summaryQueue;

	@Test
	@DisplayName("링크 생성 이벤트 수신 시 큐에 추가한다")
	void shouldAddToQueueWhenLinkCreatedEventReceived() {
		// given
		Long linkId = 123L;
		LinkCreatedEvent event = new LinkCreatedEvent(linkId);

		// when
		linkEventListener.handleLinkCreated(event);

		// then
		verify(summaryQueue, times(1)).addToQueue(linkId);
	}

	@Test
	@DisplayName("여러 링크 생성 이벤트를 순차적으로 처리한다")
	void shouldHandleMultipleLinkCreatedEvents() {
		// given
		LinkCreatedEvent event1 = new LinkCreatedEvent(1L);
		LinkCreatedEvent event2 = new LinkCreatedEvent(2L);
		LinkCreatedEvent event3 = new LinkCreatedEvent(3L);

		// when
		linkEventListener.handleLinkCreated(event1);
		linkEventListener.handleLinkCreated(event2);
		linkEventListener.handleLinkCreated(event3);

		// then
		verify(summaryQueue, times(1)).addToQueue(1L);
		verify(summaryQueue, times(1)).addToQueue(2L);
		verify(summaryQueue, times(1)).addToQueue(3L);
	}

	@Test
	@DisplayName("큐 추가 실패 시 최대 3번까지 재시도한다")
	void shouldRetryWhenAddToQueueFails() {
		// given
		Long linkId = 123L;
		LinkCreatedEvent event = new LinkCreatedEvent(linkId);

		// 첫 2번 실패, 3번째 성공
		willThrow(new RuntimeException("Queue full"))
			.willThrow(new RuntimeException("Queue full"))
			.willDoNothing()
			.given(summaryQueue).addToQueue(linkId);

		// when
		linkEventListener.handleLinkCreated(event);

		// then
		verify(summaryQueue, times(3)).addToQueue(linkId);
	}

	@Test
	@DisplayName("큐 추가가 3번 모두 실패하면 재시도를 중단하고 에러 로그를 남긴다")
	void shouldStopRetryingAfterMaxAttempts() {
		// given
		Long linkId = 123L;
		LinkCreatedEvent event = new LinkCreatedEvent(linkId);

		// 3번 모두 실패
		willThrow(new RuntimeException("Queue full"))
			.given(summaryQueue).addToQueue(linkId);

		// when
		linkEventListener.handleLinkCreated(event);

		// then
		verify(summaryQueue, times(3)).addToQueue(linkId); // 최대 3번 시도
	}

	@Test
	@DisplayName("첫 번째 시도에서 성공하면 재시도하지 않는다")
	void shouldNotRetryWhenFirstAttemptSucceeds() {
		// given
		Long linkId = 123L;
		LinkCreatedEvent event = new LinkCreatedEvent(linkId);

		willDoNothing().given(summaryQueue).addToQueue(linkId);

		// when
		linkEventListener.handleLinkCreated(event);

		// then
		verify(summaryQueue, times(1)).addToQueue(linkId); // 1번만 시도
	}
}

