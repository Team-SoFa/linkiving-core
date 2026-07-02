package com.sofa.linkiving.domain.link.event;

import java.util.Map;

/**
 * 링크 생성 완료 이벤트
 * 트랜잭션 커밋 이후 발행되는 이벤트
 */
public record LinkCreatedEvent(
	Long linkId,
	String email,
	Map<String, String> logContext
) {
	public LinkCreatedEvent(Long linkId, String email) {
		this(linkId, email, Map.of());
	}
}
