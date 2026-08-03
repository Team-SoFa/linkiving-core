package com.sofa.linkiving.global.analytics;

import static org.springframework.test.web.client.ExpectedCount.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class Ga4MeasurementProtocolClientTest {

	@Test
	void send_postsMeasurementProtocolPayload() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://www.google-analytics.com");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		Ga4Properties properties = new Ga4Properties(true, "G-TEST", "secret", null);
		Ga4MeasurementProtocolClient client = new Ga4MeasurementProtocolClient(builder.build(), properties);

		server.expect(once(), requestTo(
				"https://www.google-analytics.com/mp/collect?measurement_id=G-TEST&api_secret=secret"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.client_id").value("123.456"))
			.andExpect(jsonPath("$.user_id").value("42"))
			.andExpect(jsonPath("$.events[0].name").value("bookmark_save_success"))
			.andExpect(jsonPath("$.events[0].params.source").value("web"))
			.andRespond(withSuccess());

		client.send("123.456", "42", new Ga4Event("bookmark_save_success", Map.of("source", "web")));

		server.verify();
	}

	@Test
	void send_skipsWhenDisabled() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://www.google-analytics.com");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		Ga4Properties properties = new Ga4Properties(false, "G-TEST", "secret", null);
		Ga4MeasurementProtocolClient client = new Ga4MeasurementProtocolClient(builder.build(), properties);

		client.send("123.456", "42", new Ga4Event("bookmark_save_success", Map.of()));

		server.verify();
	}

	@Test
	void send_skipsWhenClientIdIsMissing() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://www.google-analytics.com");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		Ga4Properties properties = new Ga4Properties(true, "G-TEST", "secret", null);
		Ga4MeasurementProtocolClient client = new Ga4MeasurementProtocolClient(builder.build(), properties);

		client.send(" ", "42", new Ga4Event("bookmark_save_success", Map.of()));

		server.verify();
	}
}
