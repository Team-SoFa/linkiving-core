package com.sofa.linkiving.domain.link.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;

@DisplayName("MockLinkSyncClient 단위 테스트")
class MockLinkSyncClientTest {

	private final MockLinkSyncClient mockLinkSyncClient = new MockLinkSyncClient();

	@Test
	@DisplayName("syncCreate 호출 시 아무 작업도 수행하지 않고 정상 종료된다")
	void shouldNotThrowExceptionOnSyncCreate() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);

		// when & then
		assertThatCode(() -> mockLinkSyncClient.syncCreate(req))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("syncUpdate 호출 시 아무 작업도 수행하지 않고 정상 종료된다")
	void shouldNotThrowExceptionOnSyncUpdate() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);

		// when & then
		assertThatCode(() -> mockLinkSyncClient.syncUpdate(req))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("syncDelete 호출 시 아무 작업도 수행하지 않고 정상 종료된다")
	void shouldNotThrowExceptionOnSyncDelete() {
		// given
		Long linkId = 1L;

		// when & then
		assertThatCode(() -> mockLinkSyncClient.syncDelete(linkId))
			.doesNotThrowAnyException();
	}
}
