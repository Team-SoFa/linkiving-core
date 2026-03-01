package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.ai.AiSummaryClient;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;

@ExtendWith(MockitoExtension.class)
public class SummaryServiceTest {
	@InjectMocks
	private SummaryService summaryService;

	@Mock
	private SummaryQueryService summaryQueryService;

	@Mock
	private AiSummaryClient aiSummaryClient;

	@Test
	@DisplayName("createSummary 호출 시 AiSummaryClient에게 위임한다")
	void shouldCallGenerateSummaryWhenInitialSummary() {
		// given
		Long linkId = 1L;
		String url = "https://example.com";
		Format format = Format.CONCISE;
		String expectedResult = "Generated Summary";

		given(aiSummaryClient.generateSummary(linkId, url, format)).willReturn(expectedResult);

		// when
		String result = summaryService.initialSummary(linkId, url, format);

		// then
		assertThat(result).isEqualTo(expectedResult);
		verify(aiSummaryClient).generateSummary(linkId, url, format);
	}

	@Test
	@DisplayName("comparisonSummary 호출 시 AiSummaryClient에게 위임한다")
	void shouldCallComparisonSummaryWhenComparisonSummary() {
		// given
		String oldSummary = "old";
		String newSummary = "new";
		String expectedResult = "Comparison Result";

		given(aiSummaryClient.comparisonSummary(oldSummary, newSummary)).willReturn(expectedResult);

		// when
		String result = summaryService.comparisonSummary(oldSummary, newSummary);

		// then
		assertThat(result).isEqualTo(expectedResult);
		verify(aiSummaryClient).comparisonSummary(oldSummary, newSummary);
	}

	@Test
	@DisplayName("getSummary 호출 시 SummaryQueryService에게 위임한다")
	void shouldCallGetSummaryWhenGetSummary() {
		// given
		Long linkId = 1L;
		Summary mockSummary = mock(Summary.class);

		given(summaryQueryService.getSummary(linkId)).willReturn(mockSummary);

		// when
		Summary result = summaryService.getSummary(linkId);

		// then
		assertThat(result).isEqualTo(mockSummary);
		verify(summaryQueryService).getSummary(linkId);
	}
}
