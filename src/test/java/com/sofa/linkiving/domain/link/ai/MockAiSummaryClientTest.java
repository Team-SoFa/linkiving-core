package com.sofa.linkiving.domain.link.ai;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.link.enums.Format;

public class MockAiSummaryClientTest {
	private final MockAiSummaryClient client = new MockAiSummaryClient();

	@Test
	@DisplayName("DETAILED 포맷 요청 시 상세 요약 텍스트 반환")
	void generateSummary_Detailed() {
		// when
		String result = client.generateSummary(1L, "url", Format.DETAILED);

		// then
		assertThat(result).contains("[자세한 요약 (Mock)]");
		assertThat(result).contains("OpenFeign 도입");
	}

	@Test
	@DisplayName("SIMPLE 포맷 요청 시 간결한 요약 텍스트 반환")
	void generateSummary_Simple() {
		// when
		String result = client.generateSummary(1L, "url", Format.CONCISE);

		// then
		assertThat(result).contains("[간결한 요약 (Mock)]");
	}

	@Test
	@DisplayName("comparisonSummary 호출 시 변경 사항 분석 텍스트 반환")
	void comparisonSummary() {
		// when
		String result = client.comparisonSummary("old", "new");

		// then
		assertThat(result).contains("[변경 사항 분석]");
		assertThat(result).contains("보강되었습니다");
	}
}
