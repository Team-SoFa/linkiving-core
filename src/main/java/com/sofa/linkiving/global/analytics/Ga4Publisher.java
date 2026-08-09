package com.sofa.linkiving.global.analytics;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Ga4Publisher {

	private final Ga4MeasurementProtocolClient client;

	@Async("analyticsTaskExecutor")
	public void publish(AnalyticsContext context, Long memberId, Ga4Event event) {
		if (context == null || !StringUtils.hasText(context.clientId())) {
			return;
		}
		doSend(context.clientId(), memberId == null ? null : String.valueOf(memberId), event);
	}

	@Async("analyticsTaskExecutor")
	public void publish(String clientId, String userId, Ga4Event event) {
		doSend(clientId, userId, event);
	}

	private void doSend(String clientId, String userId, Ga4Event event) {
		try {
			client.send(clientId, userId, event);
		} catch (Exception exception) {
			log.warn("Failed to publish GA4 event - event={}, reason={}", event.name(), exception.getMessage());
		}
	}
}
