package com.sofa.linkiving.infra.feign;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sofa.linkiving.global.error.exception.BusinessException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.NONE,
	properties = "spring.main.allow-bean-definition-overriding=true"
)
@EnableFeignClients(clients = TestExternalClient.class)
public class OpenFeignIntegrationTest {

	private static MockWebServer mockWebServer;
	@Autowired
	TestExternalClient testExternalClient;

	@BeforeAll
	static void setUp() throws Exception {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
	}

	@AfterAll
	static void tearDown() throws Exception {
		mockWebServer.shutdown();
	}

	@DynamicPropertySource
	static void overrideProps(DynamicPropertyRegistry registry) {
		registry.add("test.external.base-url", () ->
			"http://localhost:" + mockWebServer.getPort());
	}

	@Test
	@DisplayName("정상 200 응답 시 FeignClient 통해 응답 본문 수신")
	void shouldCallExternalApiSuccessfullyWhenResponse200() {
		// given
		mockWebServer.enqueue(
			new MockResponse()
				.setResponseCode(200)
				.setBody("pong")
		);

		// when
		String result = testExternalClient.ping();

		// then
		assertThat(result).isEqualTo("pong");
	}

	@Test
	@DisplayName("HTTP 502 응답 시 GlobalFeignErrorDecoder 통해 ExternalApiErrorCode 매핑 예외 발생")
	void shouldThrowBusinessExceptionWhenBadGateway() {
		// given
		mockWebServer.enqueue(
			new MockResponse()
				.setResponseCode(502)
				.setBody("bad gateway")
		);

		// when & then
		assertThatThrownBy(() -> testExternalClient.ping())
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> {
				BusinessException be = (BusinessException)ex;
				assertThat(be.getErrorCode())
					.isEqualTo(ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);
			});
	}

	@TestConfiguration
	static class TestConfig {
		@Bean
		public CorsConfigurationSource corsConfigurationSource() {
			return new UrlBasedCorsConfigurationSource();
		}
	}
}
