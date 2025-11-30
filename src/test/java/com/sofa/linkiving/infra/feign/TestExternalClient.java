package com.sofa.linkiving.infra.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
	name = "testExternalClient",
	url = "${test.external.base-url}",
	configuration = GlobalFeignConfig.class
)
public interface TestExternalClient {

	@GetMapping("/ping")
	String ping();
}
