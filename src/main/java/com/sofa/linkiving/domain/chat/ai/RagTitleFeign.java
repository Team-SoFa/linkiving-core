package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.sofa.linkiving.domain.chat.dto.request.TitleGenerateReq;
import com.sofa.linkiving.domain.chat.dto.response.TitleGenerateRes;
import com.sofa.linkiving.infra.feign.GlobalFeignConfig;

@FeignClient(name = "ai-title-client", url = "${ai.server.url}", configuration = GlobalFeignConfig.class)
public interface RagTitleFeign {
	@PostMapping("/webhook/title-generate")
	List<TitleGenerateRes> generateTitle(@RequestBody TitleGenerateReq request);
}
