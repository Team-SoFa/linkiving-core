package com.sofa.linkiving.domain.link.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;

@Component
@Profile("test")
public class MockSummaryClient implements SummaryClient {

	@Override
	public RagInitialSummaryRes initialSummary(Long linkId, Long userId, String title, String url, String memo) {
		return new RagInitialSummaryRes("최초 요약");
	}

	@Override
	public RagRegenerateSummaryRes regenerateSummary(Long linkId, Long userId, String url, String existingSummary) {
		return new RagRegenerateSummaryRes("신규 요약", "비교 사항");
	}
}
