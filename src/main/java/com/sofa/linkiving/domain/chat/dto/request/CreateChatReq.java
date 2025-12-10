package com.sofa.linkiving.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "채팅방 생성 및 첫 대화 요청")
public record CreateChatReq(
	@NotBlank(message = "첫 대화 내용은 필수입니다.")
	@Schema(description = "채팅 시작에 사용되는 최초 대화", example = "AI 관련된 자료가 있어?")
	String firstChat
) {
}
