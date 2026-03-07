package com.sofa.linkiving.domain.link.worker;

import java.util.Optional;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.ai.SummaryClient;
import com.sofa.linkiving.domain.link.config.SummaryWorkerProperties;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.SummaryRes;
import com.sofa.linkiving.domain.link.dto.response.SummaryStatusRes;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.event.SummaryStatusEvent;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;

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
	private final SummaryService summaryService;
	private final LinkService linkService;
	private final SummaryClient summaryClient;
	private final ApplicationEventPublisher eventPublisher;

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
			Thread.sleep(properties.sleepDuration().toMillis());
			return;
		}

		Long linkId = linkIdOpt.get();
		String userEmail = null;
		log.info("Processing link for summary - linkId: {}", linkId);

		try {
			Link link = linkService.getLinkWithMember(linkId);
			userEmail = link.getMember().getEmail();

			eventPublisher.publishEvent(new SummaryStatusEvent(
				userEmail,
				SummaryStatusRes.of(linkId, SummaryStatus.PROCESSING)
			));

			RagInitialSummaryRes res = summaryClient.initialSummary(
				link.getId(),
				link.getMember().getId(),
				link.getTitle(),
				link.getUrl(),
				link.getMemo()
			);

			if (res != null) {
				Summary summary = summaryService.createInitialSummary(link, res.summary());

				eventPublisher.publishEvent(new SummaryStatusEvent(
					userEmail,
					SummaryStatusRes.completed(linkId, SummaryRes.from(summary))
				));
			} else {
				eventPublisher.publishEvent(new SummaryStatusEvent(
					userEmail,
					SummaryStatusRes.failed(linkId, "AI 서버 응답이 없습니다.")
				));
			}
		} catch (Exception e) {
			log.error("Failed to generate summary for linkId: {}", linkId, e);

			if (userEmail != null) {
				eventPublisher.publishEvent(new SummaryStatusEvent(
					userEmail,
					SummaryStatusRes.failed(linkId, "요약 처리 중 내부 오류가 발생했습니다.")
				));
			}
		}
	}
}
