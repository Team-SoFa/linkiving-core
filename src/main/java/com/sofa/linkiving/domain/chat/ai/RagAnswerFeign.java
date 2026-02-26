package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.infra.feign.GlobalFeignConfig;

@FeignClient(name = "ai-answer-client", url = "${ai.server.url}", configuration = GlobalFeignConfig.class)
public interface RagAnswerFeign {
	@PostMapping("/webhook/chat-answer")
	List<RagAnswerRes> generateAnswer(@RequestBody RagAnswerReq request);
}
