package com.sofa.linkiving.domain.link.worker;

import java.util.Optional;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.config.SummaryWorkerProperties;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.infra.feign.AiServerClient;
import com.sofa.linkiving.infra.feign.dto.SummaryRequest;
import com.sofa.linkiving.infra.feign.dto.SummaryResponse;

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
	private final LinkRepository linkRepository;
	private final SummaryRepository summaryRepository;
	private final AiServerClient aiServerClient;
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

		try {
			generateAndSaveSummary(linkId);
		} catch (Exception e) {
			log.error("Failed to generate summary for linkId: {}", linkId, e);
		}
	}

	@Transactional
	public void generateAndSaveSummary(Long linkId) {
		// 1. Link 조회
		Link link = linkRepository.findById(linkId)
			.orElseThrow(() -> new IllegalArgumentException("Link not found: " + linkId));

		log.debug("Link found - url: {}, title: {}", link.getUrl(), link.getTitle());

		// 2. RAG 서버에 요약 요청
		SummaryRequest request = SummaryRequest.of(
			link.getId(),
			link.getMember().getId(),
			link.getUrl(),
			link.getTitle(),
			link.getMemo()
		);
		log.info("Requesting summary to AI server - linkId: {}, userId: {}", request.linkId(), request.userId());
		SummaryResponse[] responses = aiServerClient.generateSummary(request);
		if (responses == null || responses.length == 0) {
			log.warn("AI server returned empty summary response - linkId: {}", linkId);
			return;
		}
		if (responses.length > 1) {
			log.warn("AI server returned multiple summaries, using the first - linkId: {}, size: {}", linkId,
				responses.length);
		}
		SummaryResponse response = responses[0];

		log.info("Summary generated for linkId: {}", linkId);

		// 3. Summary 엔티티 생성 및 저장
		boolean isFirstSummary = !summaryRepository.existsByLinkIdAndSelectedTrue(linkId);
		Summary summary = Summary.builder()
			.link(link)
			.format(Format.CONCISE)
			.content(response.summary())
			.selected(isFirstSummary)
			.build();

		summaryRepository.save(summary);
		log.info("Summary saved for linkId: {}", linkId);
	}
}
