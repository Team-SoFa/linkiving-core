package com.sofa.linkiving.domain.link.worker;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sofa.linkiving.global.logging.LogContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SummaryQueue {

	private final Queue<SummaryTask> summaryQueue = new ConcurrentLinkedQueue<>();

	/**
	 * 요약 대기 큐에 링크 ID 추가
	 */
	public void addToQueue(Long linkId) {
		summaryQueue.offer(new SummaryTask(linkId, LogContext.snapshot()));
		log.debug("Link added to summary queue - linkId={}", linkId);
	}

	/**
	 * 요약 대기 큐에서 링크 ID 꺼내기
	 */
	public Optional<Long> pollFromQueue() {
		return pollTaskFromQueue().map(SummaryTask::linkId);
	}

	public Optional<SummaryTask> pollTaskFromQueue() {
		return Optional.ofNullable(summaryQueue.poll());
	}
}
