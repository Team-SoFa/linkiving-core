package com.sofa.linkiving.domain.chat.dto.response;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class RagAnswerResTest {
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void shouldReadN8nArrayResponseAndKeepFalse() throws Exception {
		List<RagAnswerRes> responses = mapper.readValue("""
			[{"answer":"결과","linkIds":[],"reasoningSteps":[],"relatedLinks":[],"isFallback":false,
			"is_model_used":false,"execution_path":"metadata_direct","is_embedding_used":false,
			"used_fallback_path":false,"fallback_reason":"none","model_name":null,
			"rag_version":"v37-observability-20260917","has_history":false,"query_length_bucket":"short"}]
			""", new TypeReference<>() { });
		RagAnswerRes response = responses.get(0);
		assertThat(response.modelUsed()).isFalse();
		assertThat(response.embeddingUsed()).isFalse();
		assertThat(response.usedFallbackPath()).isFalse();
		assertThat(response.hasHistory()).isFalse();
		assertThat(response.executionPath()).isEqualTo("metadata_direct");
	}

	@Test
	void shouldAcceptOldFallbackFieldButSerializeOnlyNewName() throws Exception {
		for (boolean used : List.of(true, false)) {
			RagAnswerRes response = mapper.readValue(
				"{\"used_legacy_fallback\":" + used + "}", RagAnswerRes.class);
			assertThat(response.usedFallbackPath()).isEqualTo(used);
			var json = mapper.readTree(mapper.writeValueAsString(response));
			assertThat(json.get("used_fallback_path").booleanValue()).isEqualTo(used);
			assertThat(json.has("used_legacy_fallback")).isFalse();
		}
	}

	@Test
	void shouldNotInferFalseForLegacyOrNullTelemetry() throws Exception {
		for (String json : List.of("{}", "{\"is_model_used\":null,\"is_embedding_used\":null}")) {
			RagAnswerRes response = mapper.readValue(json, RagAnswerRes.class);
			assertThat(response.modelUsed()).isNull();
			assertThat(response.embeddingUsed()).isNull();
			assertThat(response.executionPath()).isNull();
		}
	}
}
