package com.sofa.linkiving.domain.link.event;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.sofa.linkiving.domain.link.dto.response.SummaryStatusRes;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryStatusEventListener 단위 테스트")
class SummaryStatusEventListenerTest {

	@InjectMocks
	private SummaryStatusEventListener summaryStatusEventListener;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Test
	@DisplayName("이벤트 수신 시 정확한 목적지(/queue/summary)로 웹소켓 메시지를 전송함")
	void shouldSendWebSocketMessageToUser() {
		// given
		String email = "test@example.com";
		SummaryStatusRes response = SummaryStatusRes.of(1L, SummaryStatus.COMPLETED);
		SummaryStatusEvent event = new SummaryStatusEvent(email, response);

		// when
		summaryStatusEventListener.handleSummaryStatusEvent(event);

		// then
		verify(messagingTemplate, times(1)).convertAndSendToUser(
			email,
			"/queue/summary",
			response
		);
	}
}
