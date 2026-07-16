package com.sofa.linkiving.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ai.client.calls 메트릭 등록 전용 팩토리.
 * 태그 키셋(client·operation·result)을 메서드 시그니처로 고정하여 등록 지점별 키셋 일탈을 컴파일 단계에서 차단한다.
 * 태그 값도 enum 으로 고정해 오타·표기 흔들림을 막는다.
 */
public final class AiClientMetrics {

	private static final String METRIC_NAME = "ai.client.calls";

	private static final String TAG_CLIENT = "client";
	private static final String TAG_OPERATION = "operation";
	private static final String TAG_RESULT = "result";

	private AiClientMetrics() {
	}

	public static Counter counter(MeterRegistry registry, Client client, Operation operation, Result result) {
		return Counter.builder(METRIC_NAME)
			.tag(TAG_CLIENT, client.getValue())
			.tag(TAG_OPERATION, operation.getValue())
			.tag(TAG_RESULT, result.getValue())
			.register(registry);
	}

	@Getter
	@RequiredArgsConstructor
	public enum Client {
		SUMMARY("summary"),
		ANSWER("answer"),
		TITLE("title"),
		LINK_SYNC("link-sync");

		private final String value;
	}

	@Getter
	@RequiredArgsConstructor
	public enum Operation {
		INITIAL("initial"),
		REGENERATE("regenerate"),
		GENERATE("generate"),
		CREATE("create"),
		UPDATE("update"),
		DELETE("delete");

		private final String value;
	}

	@Getter
	@RequiredArgsConstructor
	public enum Result {
		SUCCESS("success"),
		EMPTY("empty"),
		FAILURE("failure");

		private final String value;
	}
}
