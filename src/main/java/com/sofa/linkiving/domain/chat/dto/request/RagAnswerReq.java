package com.sofa.linkiving.domain.chat.dto.request;

import java.util.List;

import com.sofa.linkiving.domain.chat.enums.Mode;

public record RagAnswerReq(
	Long userId,
	String question,
	List<String> history,
	Mode mode
) {
}
