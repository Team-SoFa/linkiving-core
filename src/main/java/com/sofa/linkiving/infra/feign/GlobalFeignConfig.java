package com.sofa.linkiving.infra.feign;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;

@Configuration
public class GlobalFeignConfig {

	@Bean
	public ErrorDecoder globalErrorDecoder() {
		return new GlobalFeignErrorDecoder();
	}

	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.BASIC;
	}

	@Bean
	public Request.Options feignRequestOptions() {
		return new Request.Options(3000, 5000);
	}
}
