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

import com.sofa.linkiving.domain.link.ai.RagLinkSyncClient;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;
import com.sofa.linkiving.domain.link.enums.SyncAction;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LinkSyncEventListenerTest.RetryTestConfig.class)
@DisplayName("LinkSyncEventListener 재시도(Retry) 및 복구(Recover) 단위 테스트")
class LinkSyncEventListenerTest {

	@Autowired
	private LinkSyncEventListener linkSyncEventListener;

	@Autowired
	private RagLinkSyncClient linkSyncClient;

	@BeforeEach
	void setUp() {
		reset(linkSyncClient);
	}

	@Test
	@DisplayName("CREATE 액션 이벤트 수신 시 syncCreate를 호출한다")
	void shouldCallSyncCreate_WhenActionIsCreate() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);
		LinkSyncEvent event = new LinkSyncEvent(req, SyncAction.CREATE);

		// when
		linkSyncEventListener.handleLinkSyncEvent(event);

		// then
		verify(linkSyncClient, times(1)).syncCreate(req);
	}

	@Test
	@DisplayName("UPDATE 액션 이벤트 수신 시 syncUpdate를 호출한다")
	void shouldCallSyncUpdate_WhenActionIsUpdate() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);
		LinkSyncEvent event = new LinkSyncEvent(req, SyncAction.UPDATE);

		// when
		linkSyncEventListener.handleLinkSyncEvent(event);

		// then
		verify(linkSyncClient, times(1)).syncUpdate(req);
	}

	@Test
	@DisplayName("DELETE 액션 이벤트 수신 시 syncDelete를 호출한다")
	void shouldCallSyncDelete_WhenActionIsDelete() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);
		when(req.linkId()).thenReturn(1L);
		LinkSyncEvent event = new LinkSyncEvent(req, SyncAction.DELETE);

		// when
		linkSyncEventListener.handleLinkSyncEvent(event);

		// then
		verify(linkSyncClient, times(1)).syncDelete(1L);
	}

	@Test
	@DisplayName("동기화 실패 시 설정된 횟수(최대 3번)만큼 재시도한다")
	void shouldRetryUpTo3Times_WhenFails() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);
		LinkSyncEvent event = new LinkSyncEvent(req, SyncAction.CREATE);

		doThrow(new RuntimeException("AI Server Error")).when(linkSyncClient).syncCreate(any());

		// when
		assertThatCode(() -> linkSyncEventListener.handleLinkSyncEvent(event))
			.doesNotThrowAnyException();

		// then
		verify(linkSyncClient, times(3)).syncCreate(req);
	}

	@Test
	@DisplayName("3번 내에 성공하면 정상 종료된다 (2번 실패 후 3번째 성공)")
	void shouldNotThrowError_WhenSucceedsWithin3Times() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);
		LinkSyncEvent event = new LinkSyncEvent(req, SyncAction.UPDATE);

		doThrow(new RuntimeException("AI Server Error"))
			.doThrow(new RuntimeException("AI Server Error"))
			.doNothing()
			.when(linkSyncClient).syncUpdate(any());

		// when & then
		assertThatCode(() -> linkSyncEventListener.handleLinkSyncEvent(event))
			.doesNotThrowAnyException();

		verify(linkSyncClient, times(3)).syncUpdate(req);
	}

	/**
	 * 스프링의 @Retryable 동작을 테스트하기 위한 설정 클래스.
	 * @Async를 비활성화하여 테스트 메서드 내에서 동기적으로 재시도 로직을 검증.
	 */
	@Configuration
	@EnableRetry
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class RetryTestConfig {

		@Bean
		public RagLinkSyncClient linkSyncClient() {
			return mock(RagLinkSyncClient.class);
		}

		@Bean
		public LinkSyncEventListener linkSyncEventListener(RagLinkSyncClient linkSyncClient) {
			return new LinkSyncEventListener(linkSyncClient);
		}
	}
}
