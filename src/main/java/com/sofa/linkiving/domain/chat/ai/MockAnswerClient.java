package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;

@Component
@Profile("test")
public class MockAnswerClient implements AnswerClient {

	@Override
	public RagAnswerRes generateAnswer(RagAnswerReq request) {
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
