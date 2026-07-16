package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.chat.dto.request.TitleGenerateReq;
import com.sofa.linkiving.domain.chat.dto.response.TitleGenerateRes;
import com.sofa.linkiving.global.metrics.AiClientMetrics;
import com.sofa.linkiving.global.metrics.AiClientMetrics.Client;
import com.sofa.linkiving.global.metrics.AiClientMetrics.Operation;
import com.sofa.linkiving.global.metrics.AiClientMetrics.Result;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class RagTitleClient implements TitleClient {

	private static final int MAX_TITLE_LENGTH = 100;
	private static final Client CLIENT = Client.TITLE;
	private static final Operation OPERATION = Operation.GENERATE;

	private final RagTitleFeign ragTitleFeign;
	private final MeterRegistry meterRegistry;

	private Counter successCounter;
	private Counter emptyCounter;
	private Counter failureCounter;

	@PostConstruct
	private void initCounters() {
		this.successCounter = buildCounter(Result.SUCCESS);
		this.emptyCounter = buildCounter(Result.EMPTY);
		this.failureCounter = buildCounter(Result.FAILURE);
	}

	private Counter buildCounter(Result result) {
		return AiClientMetrics.counter(meterRegistry, CLIENT, OPERATION, result);
	}

	@Override
	public String generateTitle(String firstChat) {
		try {
			List<TitleGenerateRes> response = ragTitleFeign.generateTitle(new TitleGenerateReq(firstChat));

			if (response == null || response.isEmpty()) {
				emptyCounter.increment();
				return truncateTitle(firstChat);
			}

			successCounter.increment();
			return response.get(0).title();

		} catch (Exception e) {
			log.error("AI 서버 통신 실패. 기본 제목으로 대체합니다. error={}", e.getMessage());
			failureCounter.increment();
			return truncateTitle(firstChat);
		}
	}

	private String truncateTitle(String originalTitle) {
		if (originalTitle.length() <= MAX_TITLE_LENGTH) {
			return originalTitle;
		}
		return originalTitle.substring(0, MAX_TITLE_LENGTH);
	}
}
