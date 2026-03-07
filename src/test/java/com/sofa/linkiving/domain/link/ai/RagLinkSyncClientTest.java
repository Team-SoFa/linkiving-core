package com.sofa.linkiving.domain.link.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncDeleteReq;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkSyncClient 단위 테스트")
class RagLinkSyncClientTest {

	@InjectMocks
	private RagLinkSyncClient ragLinkSyncClient;

	@Mock
	private LinkSyncFeign linkSyncFeign;

	@Test
	@DisplayName("CREATE 동기화 시 Feign Client의 syncUpdate를 호출한다")
	void shouldCallSyncUpdateOnCreate() {
		// given
		LinkSyncUpdateReq req = mock(LinkSyncUpdateReq.class);

		// when
		ragLinkSyncClient.syncCreate(req);

		// then
		verify(linkSyncFeign, times(1)).syncUpdate(req);
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
	}
}
