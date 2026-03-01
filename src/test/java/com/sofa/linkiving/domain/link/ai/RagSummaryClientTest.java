package com.sofa.linkiving.domain.link.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.dto.request.RagInitialSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.RagRegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagSummaryClient 단위 테스트")
public class RagSummaryClientTest {

	@InjectMocks
	private RagSummaryClient ragSummaryClient;

	@Mock
	private RagSummaryFeign ragSummaryFeign;

	@Test
	@DisplayName("최초 요약 요청 성공 시 응답 객체를 반환한다")
	void shouldReturnInitialSummaryResWhenSuccess() {
		// given
		Long linkId = 1L;
		Long userId = 100L;
		String title = "Test Title";
		String url = "https://test.com";
		String memo = "Test Memo";

		RagInitialSummaryRes expectedRes = new RagInitialSummaryRes("요약 내용");
		List<RagInitialSummaryRes> responseList = List.of(expectedRes);

		given(ragSummaryFeign.requestInitialSummary(any(RagInitialSummaryReq.class)))
			.willReturn(responseList);

		// when
		RagInitialSummaryRes result = ragSummaryClient.initialSummary(linkId, userId, title, url, memo);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(expectedRes);

		verify(ragSummaryFeign, times(1)).requestInitialSummary(any(RagInitialSummaryReq.class));
	}

	@Test
	@DisplayName("최초 요약 요청 시 응답이 비어있으면 null을 반환한다")
	void shouldReturnNullWhenInitialSummaryResponseIsEmpty() {
		// given
		given(ragSummaryFeign.requestInitialSummary(any(RagInitialSummaryReq.class)))
			.willReturn(Collections.emptyList());

		// when
		RagInitialSummaryRes result = ragSummaryClient.initialSummary(1L, 100L, "Title", "URL", "Memo");

		// then
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("최초 요약 요청 중 예외 발생 시 로그를 남기고 null을 반환한다")
	void shouldReturnNullWhenInitialSummaryThrowsException() {
		// given
		given(ragSummaryFeign.requestInitialSummary(any(RagInitialSummaryReq.class)))
			.willThrow(new RuntimeException("AI Server Error"));

		// when
		RagInitialSummaryRes result = ragSummaryClient.initialSummary(1L, 100L, "Title", "URL", "Memo");

		// then
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("요약 재생성 요청 성공 시 응답 객체를 반환한다")
	void shouldReturnRegenerateSummaryResWhenSuccess() {
		// given
		Long linkId = 1L;
		Long userId = 100L;
		String url = "https://test.com";
		String existingSummary = "Old Summary";

		RagRegenerateSummaryRes expectedRes = new RagRegenerateSummaryRes("New Summary", "Difference");
		List<RagRegenerateSummaryRes> responseList = List.of(expectedRes);

		given(ragSummaryFeign.requestRegenerateSummary(any(RagRegenerateSummaryReq.class)))
			.willReturn(responseList);

		// when
		RagRegenerateSummaryRes result = ragSummaryClient.regenerateSummary(linkId, userId, url, existingSummary);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(expectedRes);

		verify(ragSummaryFeign, times(1)).requestRegenerateSummary(any(RagRegenerateSummaryReq.class));
	}

	@Test
	@DisplayName("요약 재생성 요청 중 예외 발생 시 null을 반환한다")
	void shouldReturnNullWhenRegenerateSummaryThrowsException() {
		// given
		given(ragSummaryFeign.requestRegenerateSummary(any(RagRegenerateSummaryReq.class)))
			.willThrow(new RuntimeException("Connection Timeout"));

		// when
		RagRegenerateSummaryRes result = ragSummaryClient.regenerateSummary(1L, 100L, "URL", "Old");

		// then
		assertThat(result).isNull();
	}
}
