package com.sofa.linkiving.domain.link.dto.response;

import com.sofa.linkiving.domain.link.enums.SummaryStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record SummaryStatusRes(
	@Schema(description = "링크 ID")
	Long linkId,
	@Schema(description = "요약 진행 상태:PENDING(큐 대기 중), PROCESSING(요약 진행 중), COMPLETED(완료), FAILED(생성 실패)")
	SummaryStatus status,
	@Schema(description = "요약 정보(COMPLETED 상태일 때만 존재)")
	SummaryRes summary,
	@Schema(description = "애러 메세지(FAILED 상태일 때만 존재)")
	String errorMessage
) {
	public static SummaryStatusRes of(Long linkId, SummaryStatus status) {
		return SummaryStatusRes.builder().linkId(linkId).status(status).build();
	}

	public static SummaryStatusRes completed(Long linkId, SummaryRes summary) {
		return SummaryStatusRes.builder()
			.linkId(linkId)
			.status(SummaryStatus.COMPLETED)
			.summary(summary)
			.build();
	}

	public static SummaryStatusRes failed(Long linkId, String errorMessage) {
		return SummaryStatusRes.builder()
			.linkId(linkId)
			.status(SummaryStatus.FAILED)
			.errorMessage(errorMessage)
			.build();
	}
}
