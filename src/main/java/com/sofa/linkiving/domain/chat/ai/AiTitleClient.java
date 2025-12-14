package com.sofa.linkiving.domain.chat.ai;

public interface AiTitleClient {
	/**
	 * AI 서버에 요약 요청을 보냅니다.
	 * @param firstChat 채팅 시작 대화
	 * @return 제목
	 */
	String generateSummary(String firstChat);
}
