package com.sofa.linkiving.domain.link.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.global.config.jackson.HashidsSerializer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record SummaryStatusRes(
	@Schema(description = "링크 ID")
	@JsonSerialize(using = HashidsSerializer.class)
	Long linkId,
	@Schema(description = "요약 진행 상태:PENDING(큐 대기 중), PROCESSING(요약 진행 중), COMPLETED(완료), FAILED(생성 실패)")
	SummaryStatus status,
	@Schema(description = "완료된 요약 데이터")
	SummaryRes summary,
	@Schema(description = "실패 에러 메세지")
	String errorMessage
) {
	public static SummaryStatusRes of(Long linkId, SummaryStatus status) {
		return SummaryStatusRes.builder()
			.linkId(linkId)
			.status(status)
			.summary(null)
			.errorMessage(null)
			.build();
	}

	public static SummaryStatusRes completed(Long linkId, SummaryRes summary) {
		return SummaryStatusRes.builder()
			.linkId(linkId)
			.status(SummaryStatus.COMPLETED)
			.summary(summary)
			.errorMessage(null)
			.build();
	}

	public static SummaryStatusRes failed(Long linkId, String errorMessage) {
		return SummaryStatusRes.builder()
			.linkId(linkId)
			.status(SummaryStatus.FAILED)
			.summary(null)
			.errorMessage(errorMessage)
			.build();
	}
}
