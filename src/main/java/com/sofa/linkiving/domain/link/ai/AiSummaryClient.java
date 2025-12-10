package com.sofa.linkiving.domain.link.ai;

import com.sofa.linkiving.domain.link.enums.Format;

public interface AiSummaryClient {
	/**
	 * AI 서버에 요약 요청을 보냅니다.
	 * @param linkId 링크 ID
	 * @param url 요약할 URL
	 * @param format 요약 모드
	 * @return 요약된 텍스트
	 */
	String generateSummary(Long linkId, String url, Format format);

	/**
	 * 기존 요약과 신규 요약 내용을 비교합니다.
	 * @param existingSummary 기존 요약
	 * @param newSummary 신규 요약
	 * @return 요약 비교 정보
	 */
	String comparisonSummary(String existingSummary, String newSummary);
}
