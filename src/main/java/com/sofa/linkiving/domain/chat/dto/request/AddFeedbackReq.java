package com.sofa.linkiving.domain.chat.dto.request;

import com.sofa.linkiving.domain.chat.enums.Sentiment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AddFeedbackReq(
	@NotNull(message = "피드백 상태는 필수입니다.") // 필수 값 체크
	@Schema(description = "피드백 상태 (LIKE, DISLIKE)", example = "LIKE")
	Sentiment sentiment,
	@Schema(description = "피드백 내용 (선택)", example = "도움이 되었습니다.")
	String text
) {
}
