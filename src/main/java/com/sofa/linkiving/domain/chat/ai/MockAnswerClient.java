package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("test")
public class MockAnswerClient implements AnswerClient {

	@Override
	public RagAnswerRes generateAnswer(RagAnswerReq request) {
		log.info("[Mock AI Request] User: {}, Question: {}, Mode: {}, HistoryCnt: {}",
			request.userId(), request.question(), request.mode(), request.history().size());

		return new RagAnswerRes(
			"임시 답변",
			List.of("3", "4"),
			List.of(
				new RagAnswerRes.ReasoningStep(
					"임시 답변 스탭",
					List.of("3", "4")
				)
			),
			List.of("3", "4"),
			false
		);
	}
}
