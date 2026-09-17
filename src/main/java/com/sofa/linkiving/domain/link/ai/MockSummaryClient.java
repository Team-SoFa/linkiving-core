package com.sofa.linkiving.domain.link.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.RagInitialSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.RagRegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;

@Component
@Profile("test")
public class MockSummaryClient implements SummaryClient {

	@Override
	public RagInitialSummaryRes initialSummary(RagInitialSummaryReq request) {
		return new RagInitialSummaryRes("최초 요약");
	}

	@Override
	public RagRegenerateSummaryRes regenerateSummary(RagRegenerateSummaryReq request) {
		return new RagRegenerateSummaryRes("신규 요약", "비교 사항");
	}
}
