package com.sofa.linkiving.global.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Ga4MeasurementProtocolClient {

	private final RestClient ga4RestClient;
	private final Ga4Properties properties;

	public void send(String clientId, String userId, Ga4Event event) {
		if (!properties.isReady()) {
			log.debug("Skip GA4 event because analytics.ga4 is disabled or incomplete - event={}", event.name());
			return;
		}

		if (clientId == null || clientId.isBlank()) {
			log.debug("Skip GA4 event because client_id is missing - event={}", event.name());
			return;
		}

		Map<String, Object> payload = new HashMap<>();
		Map<String, Object> eventParams = new HashMap<>(event.params());
		if (properties.debugMode()) {
			eventParams.put("debug_mode", true);
		}
		payload.put("client_id", clientId);
		payload.put("events", List.of(Map.of(
				"name", event.name(),
				"params", eventParams
		)));

		if (userId != null && !userId.isBlank()) {
			payload.put("user_id", userId);
		}

		ga4RestClient.post()
			.uri(uriBuilder -> uriBuilder
				.path("/mp/collect")
				.queryParam("measurement_id", properties.measurementId())
				.queryParam("api_secret", properties.apiSecret())
				.build())
			.body(payload)
			.retrieve()
			.toBodilessEntity();
	}
}
