package com.sofa.linkiving.domain.link.worker;

import java.util.Optional;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.config.SummaryWorkerProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableConfigurationProperties(SummaryWorkerProperties.class)
@RequiredArgsConstructor
public class SummaryWorker {

	private final SummaryQueue summaryQueue;
	private final SummaryWorkerProperties properties;
	private volatile boolean running = true;
	private Thread workerThread;

	@PostConstruct
	public void startWorker() {
		workerThread = new Thread(() -> {
			log.info("Summary worker thread started");
			while (running) {
				try {
					processQueue();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log.info("Summary worker thread interrupted");
					break;
				} catch (Exception e) {
					log.error("Error in summary worker thread", e);
				}
			}
			log.info("Summary worker thread stopped");
		});
		workerThread.setName("summary-worker");
		workerThread.setDaemon(true);
		workerThread.start();
	}

	@PreDestroy
	public void stopWorker() {
		log.info("Stopping summary worker thread");
		running = false;
		if (workerThread != null) {
			workerThread.interrupt();
		}
	}

	private void processQueue() throws InterruptedException {
		Optional<Long> linkIdOpt = summaryQueue.pollFromQueue();

		if (linkIdOpt.isEmpty()) {
			// 큐가 비어있으면 대기
			Thread.sleep(properties.sleepDuration().toMillis());
			return;
		}

		Long linkId = linkIdOpt.get();
		log.info("Processing link for summary - linkId: {}", linkId);

		// TODO: 링크 정보 조회 후 RAG 서버에 요약 요청
		// 일단은 로그만 출력
		log.debug("TODO: Send to RAG server - linkId: {}", linkId);
	}
}
