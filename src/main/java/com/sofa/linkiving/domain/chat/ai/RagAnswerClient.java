package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RagAnswerClient implements AnswerClient {

	private final RagAnswerFeign ragAnswerFeign;

	@Override
	public RagAnswerRes generateAnswer(RagAnswerReq request) {
		try {
			List<RagAnswerRes> ragAnswerRes = ragAnswerFeign.generateAnswer(request);
			log.info("RagAnswerClient generateAnswer ragAnswerRes={}", ragAnswerRes);
			return ragAnswerRes.get(0);
		} catch (Exception e) {
			log.error("RagAnswerClient generateAnswer error", e);
			return null;
		}
	}
}
