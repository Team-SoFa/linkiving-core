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
		log.info("Link created event received (after commit) - linkId: {}", event.linkId());

		int maxRetries = 3;
		int retryCount = 0;
		boolean success = false;

		while (retryCount < maxRetries && !success) {
			try {
				summaryQueue.addToQueue(event.linkId());
				success = true;
			} catch (Exception e) {
				retryCount++;
				log.warn("Failed to add link to summary queue (attempt {}/{}): linkId={}, error={}",
					retryCount, maxRetries, event.linkId(), e.getMessage());

				if (retryCount >= maxRetries) {
					// 최종 실패 시 에러 로그 및 모니터링 알림
					log.error("Failed to add link to summary queue after {} retries - linkId: {}. "
							+ "Summary generation will be skipped for this link.",
						maxRetries, event.linkId(), e);
					// TODO: 관리자 알림 또는 실패 큐에 저장하여 수동 처리 가능하도록 개선 필요
				} else {
					// 재시도 전 짧은 대기
					try {
						Thread.sleep(100L * retryCount); // 100ms, 200ms, 300ms
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						log.error("Retry interrupted for linkId: {}", event.linkId());
						break;
					}
				}
			}
		}
	}
}
