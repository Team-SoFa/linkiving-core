package com.sofa.linkiving.domain.link.dto.response;

import java.time.LocalDateTime;

import com.sofa.linkiving.domain.link.entity.SummaryDeadLetter;
import com.sofa.linkiving.domain.link.enums.DeadLetterStatus;

public record SummaryDeadLetterRes(
	Long id,
	Long linkId,
	Long memberId,
	String errorCode,
	String exceptionType,
	String failureReason,
	String requestId,
	String traceId,
	DeadLetterStatus status,
	LocalDateTime createdAt,
	LocalDateTime reprocessedAt
) {
	public static SummaryDeadLetterRes from(SummaryDeadLetter entity) {
		return new SummaryDeadLetterRes(
			entity.getId(),
			entity.getLinkId(),
			entity.getMemberId(),
			entity.getErrorCode(),
			entity.getExceptionType(),
			entity.getFailureReason(),
			entity.getRequestId(),
			entity.getTraceId(),
			entity.getStatus(),
			entity.getCreatedAt(),
			entity.getReprocessedAt()
		);
	}
}
