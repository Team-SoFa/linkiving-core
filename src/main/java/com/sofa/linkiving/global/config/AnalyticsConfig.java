package com.sofa.linkiving.global.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.sofa.linkiving.global.analytics.Ga4Properties;

@Configuration
@EnableConfigurationProperties(Ga4Properties.class)
public class AnalyticsConfig {

	@Bean
	public RestClient ga4RestClient(RestClient.Builder builder, Ga4Properties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(2));
		requestFactory.setReadTimeout(Duration.ofSeconds(3));

		return builder
			.requestFactory(requestFactory)
			.baseUrl(properties.endpoint())
			.build();
	}
}
