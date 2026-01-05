package com.sofa.linkiving.domain.link.ai;

import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;

public interface SummaryClient {
	/**
	 * AI 서버에 최초 요약 요청을 보냅니다.
	 * @param linkId 링크 ID
	 * @param userId 유저 ID
	 * @param url 요약할 URL
	 * @param title 제목
	 * @param memo 메모
	 * @return 요약된 텍스트
	 */
	RagInitialSummaryRes initialSummary(Long linkId, Long userId, String title, String url, String memo);

	/**
	 * 요약 재생성 및 기존 요약과의 차이점 요청을 보냅니다.
	 * @param linkId 링크 ID
	 * @param userId 유저 ID
	 * @param url 요약할 URL
	 * @return 요약 비교 정보
	 */
	RagRegenerateSummaryRes regenerateSummary(Long linkId, Long userId, String url, String existingSummary);
}
