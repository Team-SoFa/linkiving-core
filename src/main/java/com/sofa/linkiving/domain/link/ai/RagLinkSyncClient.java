package com.sofa.linkiving.domain.link.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncDeleteReq;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RagLinkSyncClient implements LinkSyncClient {

	private final LinkSyncFeign linkSyncFeign;
	private final MeterRegistry meterRegistry;
	private Counter createSuccess;
	private Counter createFailure;
	private Counter updateSuccess;
	private Counter updateFailure;
	private Counter deleteSuccess;
	private Counter deleteFailure;

	@PostConstruct
	private void initCounters() {
		this.createSuccess = buildCounter("create", "success");
		this.createFailure = buildCounter("create", "failure");
		this.updateSuccess = buildCounter("update", "success");
		this.updateFailure = buildCounter("update", "failure");
		this.deleteSuccess = buildCounter("delete", "success");
		this.deleteFailure = buildCounter("delete", "failure");
	}

	private Counter buildCounter(String operation, String result) {
		return Counter.builder("ai.client.calls")
			.tag("client", "link-sync")
			.tag("operation", operation)
			.tag("result", result)
			.register(meterRegistry);
	}

	@Override
	public void syncCreate(LinkSyncUpdateReq req) {
		try {
			linkSyncFeign.syncUpdate(req);
			createSuccess.increment();
			log.info("AI 서버 동기화 완료 (CREATE) - linkId: {}", req.linkId());
		} catch (Exception e) {
			createFailure.increment();
			throw e;
		}
	}

	@Override
	public void syncUpdate(LinkSyncUpdateReq req) {
		try {
			linkSyncFeign.syncUpdate(req);
			updateSuccess.increment();
			log.info("AI 서버 동기화 완료 (UPDATE) - linkId: {}", req.linkId());
		} catch (Exception e) {
			updateFailure.increment();
			throw e;
		}
	}

	@Override
	public void syncDelete(Long linkId) {
		try {
			linkSyncFeign.syncDelete(new LinkSyncDeleteReq(linkId));
			deleteSuccess.increment();
			log.info("AI 서버 동기화 완료 (DELETE) - linkId: {}", linkId);
		} catch (Exception e) {
			deleteFailure.increment();
			throw e;
		}
	}
}
