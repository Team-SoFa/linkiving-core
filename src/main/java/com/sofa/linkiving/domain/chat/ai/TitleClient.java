package com.sofa.linkiving.domain.chat.ai;

public interface TitleClient {
	/**
	 * AI 서버에 첫 채팅 내용을 토대로 채팅방 제목 생성 요청을 보냅니다.
	 * @param firstChat 채팅 시작 대화
	 * @return 제목
	 */
	String generateTitle(String firstChat);
}
