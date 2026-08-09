package com.sofa.linkiving.global.analytics;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class Ga4PublisherTest {

	@Test
	void publish_sendsEventToMeasurementProtocolClient() {
		Ga4MeasurementProtocolClient client = mock(Ga4MeasurementProtocolClient.class);
		Ga4Publisher publisher = new Ga4Publisher(client);
		Ga4Event event = new Ga4Event("link_save_success", Map.of("source", "web"));

		publisher.publish("123.456", "42", event);

		verify(client).send("123.456", "42", event);
	}

	@Test
	void publishWithContext_sendsEventToMeasurementProtocolClient() {
		Ga4MeasurementProtocolClient client = mock(Ga4MeasurementProtocolClient.class);
		Ga4Publisher publisher = new Ga4Publisher(client);
		Ga4Event event = new Ga4Event("link_save_success", Map.of("source", "web"));

		publisher.publish(AnalyticsContext.of("123.456", "web"), 42L, event);

		verify(client).send("123.456", "42", event);
	}

	@Test
	void publishWithContext_skipsWhenClientIdIsMissing() {
		Ga4MeasurementProtocolClient client = mock(Ga4MeasurementProtocolClient.class);
		Ga4Publisher publisher = new Ga4Publisher(client);
		Ga4Event event = new Ga4Event("link_save_success", Map.of("source", "web"));

		publisher.publish(AnalyticsContext.of(null, "web"), 42L, event);
		publisher.publish(AnalyticsContext.of(" ", "web"), 42L, event);
		publisher.publish(null, 42L, event);

		then(client).shouldHaveNoInteractions();
	}

	@Test
	void publish_doesNotPropagateClientException() {
		Ga4MeasurementProtocolClient client = mock(Ga4MeasurementProtocolClient.class);
		Ga4Event event = new Ga4Event("link_save_success", Map.of("source", "web"));
		willThrow(new RuntimeException("ga4 unavailable")).given(client).send("123.456", "42", event);
		Ga4Publisher publisher = new Ga4Publisher(client);

		assertThatCode(() -> publisher.publish("123.456", "42", event)).doesNotThrowAnyException();

		verify(client).send("123.456", "42", event);
	}
}
