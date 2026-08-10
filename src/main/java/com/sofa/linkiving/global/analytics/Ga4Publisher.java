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

	/**
	 * 회원 하드 삭제 전에 이벤트 전송을 시도할 수 있도록 호출 스레드에서 동기 실행한다.
	 * 전송 실패는 {@link #doSend(String, String, Ga4Event)}에서 흡수한다.
	 */
	public void publishBestEffort(String clientId, String userId, Ga4Event event) {
		if (!StringUtils.hasText(clientId)) {
			return;
		}
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
