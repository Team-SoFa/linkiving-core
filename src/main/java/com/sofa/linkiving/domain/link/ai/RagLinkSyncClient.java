package com.sofa.linkiving.domain.link.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncDeleteReq;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;
import com.sofa.linkiving.global.metrics.AiClientMetrics;
import com.sofa.linkiving.global.metrics.AiClientMetrics.Client;
import com.sofa.linkiving.global.metrics.AiClientMetrics.Operation;
import com.sofa.linkiving.global.metrics.AiClientMetrics.Result;

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

	private static final Client CLIENT = Client.LINK_SYNC;

	private final LinkSyncFeign linkSyncFeign;
	private final MeterRegistry meterRegistry;
	private Counter createSuccess;
	private Counter createFailure;
	private Counter updateSuccess;
	private Counter updateFailure;
	private Counter deleteSuccess;
	private Counter deleteFailure;

	/*
	 * link-sync 는 void 반환이라 '빈 응답' 결과가 도메인에 존재하지 않는다.
	 * 따라서 result 값은 success/failure 2종만 사용한다(의도적).
	 * 통일 대상은 태그 키셋(client·operation·result)이며, 값 도메인은 각 클라이언트 성격에 따른다.
	 */
	@PostConstruct
	private void initCounters() {
		this.createSuccess = buildCounter(Operation.CREATE, Result.SUCCESS);
		this.createFailure = buildCounter(Operation.CREATE, Result.FAILURE);
		this.updateSuccess = buildCounter(Operation.UPDATE, Result.SUCCESS);
		this.updateFailure = buildCounter(Operation.UPDATE, Result.FAILURE);
		this.deleteSuccess = buildCounter(Operation.DELETE, Result.SUCCESS);
		this.deleteFailure = buildCounter(Operation.DELETE, Result.FAILURE);
	}

	private Counter buildCounter(Operation operation, Result result) {
		return AiClientMetrics.counter(meterRegistry, CLIENT, operation, result);
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
