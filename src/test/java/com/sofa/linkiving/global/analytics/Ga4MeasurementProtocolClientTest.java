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
		Ga4Properties properties = new Ga4Properties(true, "G-TEST", "secret", null, false);
		Ga4MeasurementProtocolClient client = new Ga4MeasurementProtocolClient(builder.build(), properties);

		server.expect(once(), requestTo(
				"https://www.google-analytics.com/mp/collect?measurement_id=G-TEST&api_secret=secret"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.client_id").value("123.456"))
			.andExpect(jsonPath("$.user_id").value("42"))
			.andExpect(jsonPath("$.events[0].name").value("link_save_success"))
			.andExpect(jsonPath("$.events[0].params.source").value("web"))
			.andExpect(jsonPath("$.events[0].params.debug_mode").doesNotExist())
			.andRespond(withSuccess());

		client.send("123.456", "42", new Ga4Event("link_save_success", Map.of("source", "web")));

		server.verify();
	}

	@Test
	void send_skipsWhenDisabled() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://www.google-analytics.com");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		Ga4Properties properties = new Ga4Properties(false, "G-TEST", "secret", null, false);
		Ga4MeasurementProtocolClient client = new Ga4MeasurementProtocolClient(builder.build(), properties);

		client.send("123.456", "42", new Ga4Event("link_save_success", Map.of()));

		server.verify();
	}

	@Test
	void send_omitsUserIdWhenMissing() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://www.google-analytics.com");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		Ga4Properties properties = new Ga4Properties(true, "G-TEST", "secret", null, false);
		Ga4MeasurementProtocolClient client = new Ga4MeasurementProtocolClient(builder.build(), properties);

		server.expect(once(), requestTo(
				"https://www.google-analytics.com/mp/collect?measurement_id=G-TEST&api_secret=secret"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.client_id").value("123.456"))
			.andExpect(jsonPath("$.user_id").doesNotExist())
			.andExpect(jsonPath("$.events[0].name").value("link_save_success"))
			.andRespond(withSuccess());

		client.send("123.456", null, new Ga4Event("link_save_success", Map.of("source", "web")));

		server.verify();
	}

	@Test
	void send_skipsWhenClientIdIsMissing() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://www.google-analytics.com");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		Ga4Properties properties = new Ga4Properties(true, "G-TEST", "secret", null, false);
		Ga4MeasurementProtocolClient client = new Ga4MeasurementProtocolClient(builder.build(), properties);

		client.send(" ", "42", new Ga4Event("link_save_success", Map.of()));

		server.verify();
	}

	@Test
	void send_addsDebugModeWithoutMutatingEventParamsWhenEnabled() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://www.google-analytics.com");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		Ga4Properties properties = new Ga4Properties(true, "G-TEST", "secret", null, true);
		Ga4MeasurementProtocolClient client = new Ga4MeasurementProtocolClient(builder.build(), properties);
		Map<String, Object> originalParams = Map.of("source", "web");

		server.expect(once(), requestTo(
				"https://www.google-analytics.com/mp/collect?measurement_id=G-TEST&api_secret=secret"))
			.andExpect(jsonPath("$.events[0].params.source").value("web"))
			.andExpect(jsonPath("$.events[0].params.debug_mode").value(true))
			.andRespond(withSuccess());

		client.send("123.456", "42", new Ga4Event("query_submit", originalParams));

		assertThat(originalParams).doesNotContainKey("debug_mode");
		server.verify();
	}
}
