package com.sofa.linkiving.domain.chat.dto.response;

import java.util.List;

public record RagAnswerRes(
	String answer,
	List<String> linkIds,
	List<ReasoningStep> reasoningSteps,
	List<String> relatedLinks,
	boolean isFallback
) {
	public record ReasoningStep(
		String step,
		List<String> linkIds
	) {
	}
}
