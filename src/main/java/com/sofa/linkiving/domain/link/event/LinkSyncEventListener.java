package com.sofa.linkiving.domain.link.event;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sofa.linkiving.domain.link.ai.LinkSyncClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkSyncEventListener {

	private final LinkSyncClient linkSyncClient;

	@Async
	@Retryable(
		retryFor = Exception.class,
		maxAttempts = 3,
		backoff = @Backoff(delay = 1000, multiplier = 2)
	)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleLinkSyncEvent(LinkSyncEvent event) {
		log.info("AI 서버 동기화 비동기 실행 시도 - action: {}, linkId: {}", event.action(), event.req().linkId());

		switch (event.action()) {
			case CREATE -> linkSyncClient.syncCreate(event.req());
			case UPDATE -> linkSyncClient.syncUpdate(event.req());
			case DELETE -> linkSyncClient.syncDelete(event.req().linkId());
		}
	}

	@Recover
	public void recover(Exception exception, LinkSyncEvent event) {
		log.error("[CRITICAL] AI 서버 동기화 최종 실패. 수동 복구 필요 - action: {}, linkId: {}",
			event.action(), event.req().linkId(), exception);
	}
}
