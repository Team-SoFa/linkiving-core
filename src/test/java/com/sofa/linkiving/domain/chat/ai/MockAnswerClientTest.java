package com.sofa.linkiving.domain.chat.ai;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;
import com.sofa.linkiving.domain.chat.enums.Mode;

@ExtendWith(MockitoExtension.class)
@DisplayName("MockAnswerClient 단위 테스트")
public class MockAnswerClientTest {
	@InjectMocks
	private MockAnswerClient mockAnswerClient;

	@Test
	@DisplayName("입력값과 관계없이 항상 고정된 Gemini 관련 답변과 메타데이터를 반환한다")
	void shouldReturnFixedAnswer() {
		RagAnswerReq req = new RagAnswerReq(
			1L,
			"테스트 질문",
			Collections.emptyList(),
			Mode.DETAILED
		);

		// when
		RagAnswerRes res = mockAnswerClient.generateAnswer(req);

		// then
		assertThat(res).isNotNull();

		assertThat(res.answer()).contains("임시 답변");

		assertThat(res.linkIds())
			.hasSize(2)
			.containsExactly("3", "4");

		assertThat(res.reasoningSteps()).hasSize(1);
		RagAnswerRes.ReasoningStep step = res.reasoningSteps().get(0);
		assertThat(step.step()).contains("임시 답변 스탭");
		assertThat(step.linkIds()).containsExactly("3", "4");

		assertThat(res.relatedLinks())
			.hasSize(2)
			.containsExactly("3", "4");

		assertThat(res.isFallback()).isFalse();
	}
}
