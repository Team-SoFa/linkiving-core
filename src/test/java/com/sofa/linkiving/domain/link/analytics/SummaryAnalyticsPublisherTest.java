package com.sofa.linkiving.domain.link.analytics;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sofa.linkiving.global.analytics.AnalyticsContext;
import com.sofa.linkiving.global.analytics.Ga4Event;
import com.sofa.linkiving.global.analytics.Ga4Publisher;

class SummaryAnalyticsPublisherTest {

	private final Ga4Publisher ga4Publisher = mock(Ga4Publisher.class);
	private final SummaryAnalyticsPublisher publisher = new SummaryAnalyticsPublisher(ga4Publisher);

	@Test
	void publishComplete_publishesSummaryCompleteEvent() {
		// given
		long startedAtNanos = System.nanoTime() - 1_000_000;
		AnalyticsContext context = AnalyticsContext.of("123.456", "web");

		// when
		publisher.publishComplete(context, 100L, 1L, false, startedAtNanos);

		// then
		ArgumentCaptor<Ga4Event> eventCaptor = ArgumentCaptor.forClass(Ga4Event.class);
		verify(ga4Publisher).publish(eq(context), eq(100L), eventCaptor.capture());

		Ga4Event event = eventCaptor.getValue();
		assertThat(event.name()).isEqualTo("summary_complete");
		assertThat(event.params()).containsEntry("link_id", 1L);
		assertThat(event.params()).containsEntry("is_error", false);
		assertThat(event.params()).containsKey("latency_ms");
	}

	@Test
	void publishComplete_delegatesClientIdPolicyToGa4Publisher() {
		// when
		publisher.publishComplete(AnalyticsContext.of(null, "web"), 100L, 1L, true, System.nanoTime());

		// then
		then(ga4Publisher).should().publish(any(AnalyticsContext.class), eq(100L), any(Ga4Event.class));
	}
}
