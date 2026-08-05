package com.sofa.linkiving.global.analytics;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Ga4Publisher {

	private final Ga4MeasurementProtocolClient client;

	@Async("analyticsTaskExecutor")
	public void publish(String clientId, String userId, Ga4Event event) {
		try {
			client.send(clientId, userId, event);
		} catch (Exception exception) {
			log.warn("Failed to publish GA4 event - event={}, reason={}", event.name(), exception.getMessage());
		}
	}
}
