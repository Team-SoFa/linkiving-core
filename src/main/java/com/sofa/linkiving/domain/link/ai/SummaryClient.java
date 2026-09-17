package com.sofa.linkiving.domain.link.ai;

import com.sofa.linkiving.domain.link.dto.request.RagInitialSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.RagRegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;

public interface SummaryClient {
	/**
	 * AI 서버에 최초 요약 요청을 보냅니다.
	 * @param request 링크와 원본 저장 메타데이터
	 * @return 요약된 텍스트
	 */
	RagInitialSummaryRes initialSummary(RagInitialSummaryReq request);

	/**
	 * 요약 재생성 및 기존 요약과의 차이점 요청을 보냅니다.
	 * @param request 링크, 기존 요약과 원본 저장 메타데이터
	 * @return 요약 비교 정보
	 */
	RagRegenerateSummaryRes regenerateSummary(RagRegenerateSummaryReq request);
}
