package com.sofa.linkiving.domain.link.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sofa.linkiving.domain.link.worker.SummaryQueue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 링크 도메인 이벤트 리스너
 * 트랜잭션 커밋 후 이벤트를 처리하여 데이터 일관성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkEventListener {

	private final SummaryQueue summaryQueue;

	/**
	 * 링크 생성 완료 이벤트 처리
	 * 트랜잭션 커밋 후에만 실행되어 롤백 시 큐에 추가되지 않음
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleLinkCreated(LinkCreatedEvent event) {
		log.debug("Link created event received - linkId: {}", event.linkId());
		summaryQueue.addToQueue(event.linkId());
	}
}
