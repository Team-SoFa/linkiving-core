package com.sofa.linkiving.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.sofa.linkiving.global.analytics.Ga4Properties;

@Configuration
@EnableConfigurationProperties(Ga4Properties.class)
public class AnalyticsConfig {

	@Bean
	public RestClient ga4RestClient(Ga4Properties properties) {
		return RestClient.builder()
			.baseUrl(properties.endpoint())
			.build();
	}
}
