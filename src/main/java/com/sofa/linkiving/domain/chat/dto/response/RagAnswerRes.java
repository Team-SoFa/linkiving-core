package com.sofa.linkiving.domain.chat.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RagAnswerRes(
	String answer,
	List<String> linkIds,
	List<ReasoningStep> reasoningSteps,
	List<String> relatedLinks,
	boolean isFallback,
	Integer retrievedCount,
	Integer selectedCount,
	Double topSimilarity,
	@JsonProperty("is_model_used") Boolean modelUsed,
	@JsonProperty("execution_path") String executionPath,
	@JsonProperty("is_embedding_used") Boolean embeddingUsed,
	@JsonProperty("used_fallback_path") @JsonAlias("used_legacy_fallback") Boolean usedFallbackPath,
	@JsonProperty("fallback_reason") String fallbackReason,
	@JsonProperty("model_name") String modelName,
	@JsonProperty("rag_version") String ragVersion,
	@JsonProperty("has_history") Boolean hasHistory,
	@JsonProperty("query_length_bucket") String queryLengthBucket
) {
	public RagAnswerRes(
		String answer,
		List<String> linkIds,
		List<ReasoningStep> reasoningSteps,
		List<String> relatedLinks,
		boolean isFallback,
		Integer retrievedCount,
		Integer selectedCount,
		Double topSimilarity
	) {
		this(answer, linkIds, reasoningSteps, relatedLinks, isFallback, retrievedCount, selectedCount,
			topSimilarity, null, null, null, null, null, null, null, null, null);
	}

	public RagAnswerRes(
		String answer,
		List<String> linkIds,
		List<ReasoningStep> reasoningSteps,
		List<String> relatedLinks,
		boolean isFallback
	) {
		this(answer, linkIds, reasoningSteps, relatedLinks, isFallback, null, null, null);
	}

	public record ReasoningStep(
		String step,
		List<String> linkIds
	) {
	}
}
