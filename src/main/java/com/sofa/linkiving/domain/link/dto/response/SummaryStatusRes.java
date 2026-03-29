package com.sofa.linkiving.domain.link.dto.response;

import com.sofa.linkiving.domain.link.enums.SummaryStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record SummaryStatusRes<T>(
	@Schema(description = "링크 ID")
	Long linkId,
	@Schema(description = "요약 진행 상태:PENDING(큐 대기 중), PROCESSING(요약 진행 중), COMPLETED(완료), FAILED(생성 실패)")
	SummaryStatus status,
	@Schema(description = "결과 데이터 (COMPLETED 상태일 때는 SummaryRes 객체, FAILED 상태일 때는 String 에러 메세지, 그 외는 null)")
	T data
) {
	public static SummaryStatusRes<Void> of(Long linkId, SummaryStatus status) {
		return SummaryStatusRes.<Void>builder()
			.linkId(linkId)
			.status(status)
			.data(null)
			.build();
	}

	public static SummaryStatusRes<SummaryRes> completed(Long linkId, SummaryRes summary) {
		return SummaryStatusRes.<SummaryRes>builder()
			.linkId(linkId)
			.status(SummaryStatus.COMPLETED)
			.data(summary)
			.build();
	}

	public static SummaryStatusRes<String> failed(Long linkId, String errorMessage) {
		return SummaryStatusRes.<String>builder()
			.linkId(linkId)
			.status(SummaryStatus.FAILED)
			.data(errorMessage)
			.build();
	}
}
