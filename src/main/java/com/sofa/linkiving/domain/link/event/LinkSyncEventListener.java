package com.sofa.linkiving.domain.link.event;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sofa.linkiving.domain.link.ai.LinkSyncClient;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;
import com.sofa.linkiving.domain.link.enums.SyncAction;
import com.sofa.linkiving.domain.link.service.LinkSyncSnapshotService;
import com.sofa.linkiving.global.logging.LogContext;
import com.sofa.linkiving.global.metrics.AsyncTaskMetrics;
import com.sofa.linkiving.global.metrics.AsyncTaskMetrics.Action;
import com.sofa.linkiving.global.metrics.AsyncTaskMetrics.Task;
import com.sofa.linkiving.infra.feign.NonRetryableExternalApiException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkSyncEventListener {

	private final LinkSyncClient linkSyncClient;
	private final MeterRegistry meterRegistry;
	private final LinkSyncSnapshotService snapshotService;

	// 같은 JVM의 동일 링크 전송만 직렬화한다. 고정 크기로 링크 수에 따른 잠금 객체 누적을 막는다.
	private final Object[] syncLocks = IntStream.range(0, 256).mapToObj(index -> new Object()).toArray();

	private final Map<SyncAction, Counter> failureCounters = new EnumMap<>(SyncAction.class);

	@PostConstruct
	private void initCounters() {
		for (SyncAction syncAction : SyncAction.values()) {
			failureCounters.put(syncAction,
				AsyncTaskMetrics.failureCounter(meterRegistry, Task.LINK_SYNC, toMetricAction(syncAction)));
		}
	}

	private Action toMetricAction(SyncAction syncAction) {
		return switch (syncAction) {
			case CREATE -> Action.CREATE;
			case UPDATE -> Action.UPDATE;
			case DELETE -> Action.DELETE;
		};
	}

	@Async("aiTaskExecutor")
	@Retryable(
		retryFor = Exception.class,
		noRetryFor = NonRetryableExternalApiException.class,
		maxAttempts = 3,
		backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 8000, random = true)
	)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleLinkSyncEvent(LinkSyncEvent event) {
		try (LogContext.MdcScope ignored = LogContext.restore(event.logContext());
			LogContext.MdcScope ignoredLinkScope = LogContext.withLinkId(event.req().linkId())) {
			log.info("AI 서버 동기화 비동기 실행 시도 - action: {}, linkId: {}", event.action(), event.req().linkId());

			Long linkId = event.req().linkId();
			synchronized (syncLocks[Math.floorMod(linkId.hashCode(), syncLocks.length)]) {
				syncCurrentState(linkId, event.action());
			}
		}
	}

	private void syncCurrentState(Long linkId, SyncAction action) {
		if (action == SyncAction.DELETE) {
			linkSyncClient.syncDelete(linkId);
			return;
		}

		LinkSyncUpdateReq current = snapshotService.findCurrent(linkId).orElse(null);
		if (current == null) {
			// 삭제·탈퇴 후 늦게 도착한 UPDATE도 이전 payload로 링크를 되살리지 않는다.
			linkSyncClient.syncDelete(linkId);
			return;
		}
		if (action == SyncAction.CREATE) {
			linkSyncClient.syncCreate(current);
		} else {
			linkSyncClient.syncUpdate(current);
		}
		// HTTP 호출 중 삭제·탈퇴가 커밋된 경우에는 성공한 쓰기를 다시 제거한다.
		if (!snapshotService.isSyncTarget(linkId)) {
			linkSyncClient.syncDelete(linkId);
		}
	}

	@Recover
	public void recover(Exception exception, LinkSyncEvent event) {
		try (LogContext.MdcScope ignored = LogContext.restore(event.logContext());
			LogContext.MdcScope linkScope = LogContext.withLinkId(event.req().linkId())) {
			failureCounters.get(event.action()).increment();
			log.error("[CRITICAL] AI 서버 동기화 최종 실패. 수동 복구 필요 - action: {}, linkId: {}",
				event.action(), event.req().linkId(), exception);
		}
	}
}
