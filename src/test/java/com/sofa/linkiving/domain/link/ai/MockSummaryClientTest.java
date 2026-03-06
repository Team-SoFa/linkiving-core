package com.sofa.linkiving.domain.link.ai;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;

public class MockSummaryClientTest {
	private final MockSummaryClient client = new MockSummaryClient();

	@Test
	@DisplayName("generateSummary 호출 시 최초 요약 진행")
	void initialSummary_Detailed() {
		// when
		RagInitialSummaryRes result = client.initialSummary(1L, 1L, "title", "url", "memo");

		// then
		assertThat(result.summary()).isEqualTo("최초 요약");
	}

	@Test
	@DisplayName("comparisonSummary 호출 시 변경 사항 분석 텍스트 반환")
	void comparisonSummary() {
		// when
		RagRegenerateSummaryRes result = client.regenerateSummary(1L, 1L, "url", "old");

		// then
		assertThat(result.summary()).isEqualTo("신규 요약");
		assertThat(result.difference()).contains("비교 사항");
	}
}
