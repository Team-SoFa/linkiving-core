package com.sofa.linkiving.domain.link.event;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryStatusEventListener {
	private final SimpMessagingTemplate messagingTemplate;

	@Async
	@EventListener
	public void handleSummaryStatusEvent(SummaryStatusEvent event) {
		messagingTemplate.convertAndSendToUser(
			event.email(),
			"/queue/summary",
			event.response()
		);
		log.info("요약 상태 웹소켓 푸시 완료 - 이메일: {}, 상태: {}, 링크ID: {}",
			event.email(), event.response().status(), event.response().linkId());
	}
}
