package com.sofa.linkiving.domain.link.service;

import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sofa.linkiving.domain.link.entity.SummaryDeadLetter;
import com.sofa.linkiving.domain.link.enums.DeadLetterStatus;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.error.SummaryErrorCode;
import com.sofa.linkiving.domain.link.repository.SummaryDeadLetterRepository;
import com.sofa.linkiving.domain.link.worker.SummaryQueue;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.global.logging.LogContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryDeadLetterService {

	private static final int MAX_REASON_LENGTH = 1000;

	private final SummaryDeadLetterRepository deadLetterRepository;
	private final SummaryQueue summaryQueue;
	private final LinkService linkService;

	/**
	 * 요약 작업이 영구 실패했을 때 dead-letter 로 적재한다. (워커 스레드에서 호출)
	 */
	@Transactional
	public void record(Long linkId, Long memberId, Throwable cause) {
		String errorCode = null;
		if (cause instanceof BusinessException businessException) {
			errorCode = businessException.getErrorCode().getCode();
		}

		SummaryDeadLetter deadLetter = SummaryDeadLetter.builder()
			.linkId(linkId)
			.memberId(memberId)
			.errorCode(errorCode)
			.exceptionType(cause != null ? cause.getClass().getSimpleName() : null)
			.failureReason(truncate(cause != null ? cause.getMessage() : null))
			.requestId(MDC.get(LogContext.REQUEST_ID))
			.traceId(MDC.get(LogContext.TRACE_ID))
			.build();

		deadLetterRepository.save(deadLetter);
		log.info("Summary dead-letter recorded - linkId={}, code={}", linkId, errorCode);
	}

	@Transactional(readOnly = true)
	public Page<SummaryDeadLetter> getDeadLetters(DeadLetterStatus status, Pageable pageable) {
		if (status == null) {
			return deadLetterRepository.findAll(pageable);
		}
		return deadLetterRepository.findAllByStatus(status, pageable);
	}

	/**
	 * PENDING 상태의 dead-letter 를 재처리한다. 링크 상태를 PENDING 으로 되돌리고,
	 * 커밋 이후 요약 큐에 재적재한다(워커가 커밋 전 상태를 읽는 레이스 방지).
	 */
	@Transactional
	public void reprocess(Long id) {
		SummaryDeadLetter deadLetter = deadLetterRepository.findById(id)
			.orElseThrow(() -> new BusinessException(SummaryErrorCode.DEAD_LETTER_NOT_FOUND));

		if (!deadLetter.isReprocessable()) {
			throw new BusinessException(SummaryErrorCode.DEAD_LETTER_NOT_REPROCESSABLE);
		}

		Long linkId = deadLetter.getLinkId();
		linkService.updateSummaryStatus(linkId, SummaryStatus.PENDING);
		deadLetter.markReprocessed();
		enqueueAfterCommit(linkId);

		log.info("Summary dead-letter reprocess requested - id={}, linkId={}", id, linkId);
	}

	@Transactional
	public void ignore(Long id) {
		SummaryDeadLetter deadLetter = deadLetterRepository.findById(id)
			.orElseThrow(() -> new BusinessException(SummaryErrorCode.DEAD_LETTER_NOT_FOUND));
		deadLetter.markIgnored();
	}

	private void enqueueAfterCommit(Long linkId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					summaryQueue.addToQueue(linkId);
				}
			});
		} else {
			summaryQueue.addToQueue(linkId);
		}
	}

	private String truncate(String value) {
		if (value == null) {
			return null;
		}
		return value.length() > MAX_REASON_LENGTH ? value.substring(0, MAX_REASON_LENGTH) : value;
	}
}
