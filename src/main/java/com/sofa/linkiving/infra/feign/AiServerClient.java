package com.sofa.linkiving.infra.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.sofa.linkiving.infra.feign.dto.SummaryRequest;
import com.sofa.linkiving.infra.feign.dto.SummaryResponse;

@FeignClient(
	name = "aiServerClient",
	url = "${ai.server.url}",
	configuration = GlobalFeignConfig.class
)
public interface AiServerClient {

	@PostMapping("/webhook/summary-initial")
	SummaryResponse[] generateSummary(@RequestBody SummaryRequest request);
}
