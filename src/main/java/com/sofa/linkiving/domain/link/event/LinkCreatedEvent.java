package com.sofa.linkiving.domain.link.event;

/**
 * 링크 생성 완료 이벤트
 * 트랜잭션 커밋 이후 발행되는 이벤트
 */
public record LinkCreatedEvent(
	Long linkId
) {
}
