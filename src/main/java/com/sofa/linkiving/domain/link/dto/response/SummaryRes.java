package com.sofa.linkiving.domain.link.dto.response;

import com.sofa.linkiving.domain.link.entity.Summary;

import io.swagger.v3.oas.annotations.media.Schema;

public record SummaryRes(
	@Schema(description = "요약 ID")
	Long id,
	@Schema(description = "요약 내용", example = "이 링크는 예시 링크입니다.")
	String content
) {
	public static SummaryRes from(Summary summary) {
		if (summary == null) {
			return null;
		}
		return new SummaryRes(summary.getId(), summary.getContent());
	}
}
