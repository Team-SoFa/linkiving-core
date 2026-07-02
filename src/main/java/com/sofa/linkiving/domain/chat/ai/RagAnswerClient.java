package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RagAnswerClient implements AnswerClient {

	private final RagAnswerFeign ragAnswerFeign;
	private final MeterRegistry meterRegistry;

	private Counter successCounter;
	private Counter emptyCounter;
	private Counter failureCounter;

	@PostConstruct
	private void initCounters() {
		this.successCounter = buildCounter("success");
		this.emptyCounter = buildCounter("empty");
		this.failureCounter = buildCounter("failure");
	}

	private Counter buildCounter(String result) {
		return Counter.builder("ai.client.calls")
			.tag("client", "answer")
			.tag("result", result)
			.register(meterRegistry);
	}

	@Override
	public RagAnswerRes generateAnswer(RagAnswerReq request) {
		try {
			List<RagAnswerRes> ragAnswerRes = ragAnswerFeign.generateAnswer(request);

			if (ragAnswerRes == null || ragAnswerRes.isEmpty()) {
				log.warn("RagAnswerClient generateAnswer empty response");
				emptyCounter.increment();
				return null;
			}

			log.info("RagAnswerClient generateAnswer ragAnswerRes={}", ragAnswerRes);
			successCounter.increment();
			return ragAnswerRes.get(0);

		} catch (Exception e) {
			log.error("RagAnswerClient generateAnswer error", e);
			failureCounter.increment();
			return null;
		}
	}
}
