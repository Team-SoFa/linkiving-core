package com.sofa.linkiving.domain.link.worker;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SummaryQueue {

	private final Queue<Long> summaryQueue = new ConcurrentLinkedQueue<>();

	/**
	 * 요약 대기 큐에 링크 ID 추가
	 */
	public void addToQueue(Long linkId) {
		summaryQueue.offer(linkId);
		log.info("Link added to summary queue - linkId: {}", linkId);
	}

	/**
	 * 요약 대기 큐에서 링크 ID 꺼내기
	 */
	public Optional<Long> pollFromQueue() {
		return Optional.ofNullable(summaryQueue.poll());
	}
}
