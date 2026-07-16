package com.sofa.linkiving.domain.chat.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.chat.dto.request.TitleGenerateReq;
import com.sofa.linkiving.domain.chat.dto.response.TitleGenerateRes;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagTitleClient 단위 테스트")
class RagTitleClientTest {

	private RagTitleClient ragTitleClient;

	@Mock
	private RagTitleFeign ragTitleFeign;

	private SimpleMeterRegistry meterRegistry;

	@BeforeEach
	void setUp() {
		meterRegistry = new SimpleMeterRegistry();
		ragTitleClient = new RagTitleClient(ragTitleFeign, meterRegistry);
		ReflectionTestUtils.invokeMethod(ragTitleClient, "initCounters");
	}

	private double counterCount(String result) {
		return meterRegistry.counter("ai.client.calls", "client", "title", "operation", "generate", "result", result)
			.count();
	}

	@Test
	@DisplayName("AI 서버 통신 성공 시 생성된 제목을 반환한다")
	void shouldReturnGeneratedTitleWhenApiCallSucceeds() {
		// given
		String firstChat = "안녕하세요";
		String generatedTitle = "인사말";

		TitleGenerateRes resDto = mock(TitleGenerateRes.class);
		given(resDto.title()).willReturn(generatedTitle);

		given(ragTitleFeign.generateTitle(any(TitleGenerateReq.class)))
			.willReturn(List.of(resDto));

		// when
		String result = ragTitleClient.generateTitle(firstChat);

		// then
		assertThat(result).isEqualTo(generatedTitle);
		verify(ragTitleFeign).generateTitle(any(TitleGenerateReq.class));
		assertThat(counterCount("success")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("AI 서버 응답이 빈 리스트일 경우 요청한 첫 메시지(firstChat)를 그대로 반환한다")
	void shouldReturnFirstChatWhenResponseIsEmpty() {
		// given
		String firstChat = "안녕하세요";

		given(ragTitleFeign.generateTitle(any(TitleGenerateReq.class)))
			.willReturn(Collections.emptyList());

		// when
		String result = ragTitleClient.generateTitle(firstChat);

		// then
		assertThat(result).isEqualTo(firstChat);
		assertThat(counterCount("empty")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("AI 서버 응답이 null일 경우 요청한 첫 메시지(firstChat)를 그대로 반환한다")
	void shouldReturnFirstChatWhenResponseIsNull() {
		// given
		String firstChat = "안녕하세요";

		given(ragTitleFeign.generateTitle(any(TitleGenerateReq.class)))
			.willReturn(null);

		// when
		String result = ragTitleClient.generateTitle(firstChat);

		// then
		assertThat(result).isEqualTo(firstChat);
		assertThat(counterCount("empty")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("AI 서버 통신 중 예외 발생 시 로그를 남기고 첫 메시지(firstChat)를 그대로 반환한다")
	void shouldReturnFirstChatWhenExceptionOccurs() {
		// given
		String firstChat = "안녕하세요";

		given(ragTitleFeign.generateTitle(any(TitleGenerateReq.class)))
			.willThrow(new RuntimeException("API Connection Failed"));

		// when
		String result = ragTitleClient.generateTitle(firstChat);

		// then
		assertThat(result).isEqualTo(firstChat);
		assertThat(counterCount("failure")).isEqualTo(1.0);
	}
}
