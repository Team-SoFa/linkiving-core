package com.sofa.linkiving.domain.link.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;

@Component
@Profile("test")
public class MockLinkSyncClient implements LinkSyncClient {
	@Override
	public void syncCreate(LinkSyncUpdateReq req) {
		return;
	}

	@Override
	public void syncUpdate(LinkSyncUpdateReq req) {
		return;
	}

	@Override
	public void syncDelete(Long linkId) {
		return;
	}
}
