package com.sofa.linkiving.domain.link.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncDeleteReq;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RagLinkSyncClient implements LinkSyncClient {

	private final LinkSyncFeign linkSyncFeign;

	@Override
	public void syncCreate(LinkSyncUpdateReq req) {
		linkSyncFeign.syncUpdate(req);
		log.info("AI 서버 동기화 완료 (CREATE) - linkId: {}", req.linkId());
	}

	@Override
	public void syncUpdate(LinkSyncUpdateReq req) {
		linkSyncFeign.syncUpdate(req);
		log.info("AI 서버 동기화 완료 (UPDATE) - linkId: {}", req.linkId());
	}

	@Override
	public void syncDelete(Long linkId) {
		linkSyncFeign.syncDelete(new LinkSyncDeleteReq(linkId));
		log.info("AI 서버 동기화 완료 (DELETE) - linkId: {}", linkId);
	}
}
