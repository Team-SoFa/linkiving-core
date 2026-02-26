package com.sofa.linkiving.domain.chat.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagAnswerClient 단위 테스트")
class RagAnswerClientTest {

	@InjectMocks
	private RagAnswerClient ragAnswerClient;

	@Mock
	private RagAnswerFeign ragAnswerFeign;

	@Test
	@DisplayName("generateAnswer: Feign 응답이 정상일 경우 리스트의 첫 번째 요소를 반환한다")
	void shouldReturnFirstElement_WhenGenerateAnswerSuccess() {
		// given
		RagAnswerReq req = mock(RagAnswerReq.class);
		RagAnswerRes expectedRes = mock(RagAnswerRes.class);
		given(ragAnswerFeign.generateAnswer(any(RagAnswerReq.class)))
			.willReturn(List.of(expectedRes));

		// when
		RagAnswerRes actualRes = ragAnswerClient.generateAnswer(req);

		// then
		assertThat(actualRes).isEqualTo(expectedRes);
	}

	@Test
	@DisplayName("generateAnswer: Feign 요청 중 예외가 발생하면 예외를 잡고 null을 반환한다")
	void shouldCatchExceptionAndReturnNull_WhenGenerateAnswerThrowsException() {
		// given
		RagAnswerReq req = mock(RagAnswerReq.class);
		given(ragAnswerFeign.generateAnswer(any(RagAnswerReq.class)))
			.willThrow(new RuntimeException("AI Server Error"));

		// when
		RagAnswerRes actualRes = ragAnswerClient.generateAnswer(req);

		// then
		assertThat(actualRes).isNull();
	}
}
