package com.sofa.linkiving.domain.link.entity;

import java.time.LocalDateTime;

import com.sofa.linkiving.domain.link.enums.DeadLetterStatus;
import com.sofa.linkiving.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryDeadLetter extends BaseEntity {

	@Column(nullable = false)
	private Long linkId;

	private Long memberId;

	@Column(length = 32)
	private String errorCode;

	@Column(length = 128)
	private String exceptionType;

	@Column(length = 1000)
	private String failureReason;

	@Column(length = 64)
	private String requestId;

	@Column(length = 64)
	private String traceId;

	@Column(nullable = false)
	private DeadLetterStatus status;

	private LocalDateTime reprocessedAt;

	@Builder
	public SummaryDeadLetter(Long linkId, Long memberId, String errorCode, String exceptionType,
		String failureReason, String requestId, String traceId) {
		this.linkId = linkId;
		this.memberId = memberId;
		this.errorCode = errorCode;
		this.exceptionType = exceptionType;
		this.failureReason = failureReason;
		this.requestId = requestId;
		this.traceId = traceId;
		this.status = DeadLetterStatus.PENDING;
	}

	public void markReprocessed() {
		this.status = DeadLetterStatus.REPROCESSED;
		this.reprocessedAt = LocalDateTime.now();
	}

	public void markIgnored() {
		this.status = DeadLetterStatus.IGNORED;
	}

	public boolean isReprocessable() {
		return this.status == DeadLetterStatus.PENDING;
	}
}
