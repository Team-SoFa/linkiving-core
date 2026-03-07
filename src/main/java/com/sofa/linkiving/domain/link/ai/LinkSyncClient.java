package com.sofa.linkiving.domain.link.ai;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;

public interface LinkSyncClient {

	void syncCreate(LinkSyncUpdateReq req);

	void syncUpdate(LinkSyncUpdateReq req);

	void syncDelete(Long linkId);
}
