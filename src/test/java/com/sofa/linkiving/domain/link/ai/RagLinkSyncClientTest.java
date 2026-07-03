package com.sofa.linkiving.domain.link.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncDeleteReq;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagLinkSyncClient 단위 테스트")
class RagLinkSyncClientTest {

	private RagLinkSyncClient ragLinkSyncClient;

	@Mock
	private LinkSyncFeign linkSyncFeign;

	private SimpleMeterRegistry meterRegistry;

	@BeforeEach
	void setUp() {
		meterRegistry = new SimpleMeterRegistry();
		ragLinkSyncClient = new RagLinkSyncClient(linkSyncFeign, meterRegistry);
		ReflectionTestUtils.invokeMethod(ragLinkSyncClient, "initCounters");
	}

	private double callCount(String operation, String result) {
		return meterRegistry.counter("ai.client.calls",
			"client", "link-sync", "operation", operation, "result", result).count();
	}

	@Test
	@DisplayName("CREATE 동기화 시 Feign Client의 syncUpdate를 호출한다")
	void shouldCallSyncUpdateOnCreate() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);

		// when
		ragLinkSyncClient.syncCreate(req);

		// then
		verify(linkSyncFeign, times(1)).syncUpdate(req);
		assertThat(callCount("create", "success")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("UPDATE 동기화 시 Feign Client의 syncUpdate를 호출한다")
	void shouldCallSyncUpdateOnUpdate() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);

		// when
		ragLinkSyncClient.syncUpdate(req);

		// then
		verify(linkSyncFeign, times(1)).syncUpdate(req);
		assertThat(callCount("update", "success")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("DELETE 동기화 시 LinkSyncDeleteReq를 생성하여 Feign Client의 syncDelete를 호출한다")
	void shouldCallSyncDeleteOnDelete() {
		// given
		Long linkId = 10L;

		// when
		ragLinkSyncClient.syncDelete(linkId);

		// then
		ArgumentCaptor<LinkSyncDeleteReq> captor = ArgumentCaptor.forClass(LinkSyncDeleteReq.class);
		verify(linkSyncFeign, times(1)).syncDelete(captor.capture());

		LinkSyncDeleteReq capturedReq = captor.getValue();
		assertThat(capturedReq.linkId()).isEqualTo(linkId);
		assertThat(callCount("delete", "success")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("동기화 실패 시 failure 카운터를 올리고 예외를 다시 던진다")
	void shouldCountFailureAndRethrow_WhenFeignThrows() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);
		doThrow(new RuntimeException("AI Server Error")).when(linkSyncFeign).syncUpdate(req);

		// when & then : 예외를 삼키지 않고 그대로 던져야 (재시도/복구 로직 보존)
		assertThatThrownBy(() -> ragLinkSyncClient.syncCreate(req))
			.isInstanceOf(RuntimeException.class);

		assertThat(callCount("create", "failure")).isEqualTo(1.0);
	}
}
