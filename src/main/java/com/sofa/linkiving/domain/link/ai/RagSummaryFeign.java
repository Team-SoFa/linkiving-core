package com.sofa.linkiving.domain.link.ai;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.sofa.linkiving.domain.link.dto.request.RagInitialSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.RagRegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;
import com.sofa.linkiving.infra.feign.GlobalFeignConfig;

@FeignClient(name = "ai-summary-client", url = "${ai.server.url}", configuration = GlobalFeignConfig.class)
public interface RagSummaryFeign {
	@PostMapping("/webhook/summary-initial")
	List<RagInitialSummaryRes> requestInitialSummary(@RequestBody RagInitialSummaryReq req);

	@PostMapping("/webhook/summary-resummarize")
	List<RagRegenerateSummaryRes> requestRegenerateSummary(@RequestBody RagRegenerateSummaryReq request);
}
