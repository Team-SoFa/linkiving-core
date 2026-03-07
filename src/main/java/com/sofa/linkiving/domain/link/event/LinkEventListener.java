package com.sofa.linkiving.domain.link.event;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sofa.linkiving.domain.link.dto.response.SummaryStatusRes;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.facade.SummaryWorkerFacade;
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
	private final ApplicationEventPublisher eventPublisher;
	private final SummaryWorkerFacade summaryWorkerFacade;
	private final ObjectProvider<LinkEventListener> selfProvider;

	/**
	 * 트랜잭션 커밋 후 비동기로 큐 적재 실행
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleLinkCreated(LinkCreatedEvent event) {
		eventPublisher.publishEvent(new SummaryStatusEvent(
			event.email(),
			SummaryStatusRes.of(event.linkId(), SummaryStatus.PENDING)
		));

		selfProvider.getObject().addToQueueWithRetry(event);

		log.info("Link created event received & queued async - linkId: {}", event.linkId());

	}

	/**
	 * 큐 적재 전용 메서드. (Exception 발생 시 최대 3번 재시도)
	 */
	@Retryable(
		value = {Exception.class},
		maxAttempts = 3,
		backoff = @Backoff(delay = 1000)
	)
	public void addToQueueWithRetry(LinkCreatedEvent event) {
		summaryQueue.addToQueue(event.linkId());
	}

	/**
	 * 최대 재시도 횟수 초과 시 최종 실패 처리 로직
	 */
	@Recover
	public void recover(Exception exception, LinkCreatedEvent event) {
		log.error("Final failure to queue link after retries - linkId: {}", event.linkId(), exception);

		summaryWorkerFacade.updateSummaryStatus(event.linkId(), SummaryStatus.FAILED);

		eventPublisher.publishEvent(new SummaryStatusEvent(
			event.email(),
			SummaryStatusRes.failed(event.linkId(), "요약 대기열 등록에 실패했습니다.")
		));
		// TODO: 관리자 알림, 슬랙 발송 또는 실패 큐 적재 등 후속 처리
	}
}
