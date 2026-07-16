package com.sofa.linkiving.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * async.task.failures 메트릭 등록 전용 팩토리.
 * 태그 키셋(task·action)을 메서드 시그니처로 고정하여 등록 지점별 키셋 일탈을 컴파일 단계에서 차단한다.
 *
 * task 는 작업 계열, action 은 그 계열 안의 동작을 의미하는 서로 다른 차원이므로
 * 문자열로 뭉치지 않고 2차원 라벨로 유지한다.
 */
public final class AsyncTaskMetrics {

	private static final String METRIC_NAME = "async.task.failures";

	private static final String TAG_TASK = "task";
	private static final String TAG_ACTION = "action";

	private AsyncTaskMetrics() {
	}

	public static Counter failureCounter(MeterRegistry registry, Task task, Action action) {
		return Counter.builder(METRIC_NAME)
			.tag(TAG_TASK, task.getValue())
			.tag(TAG_ACTION, action.getValue())
			.register(registry);
	}

	@Getter
	@RequiredArgsConstructor
	public enum Task {
		SUMMARY("summary"),
		LINK_SYNC("link-sync");

		private final String value;
	}

	@Getter
	@RequiredArgsConstructor
	public enum Action {
		GENERATE("GENERATE"),
		ENQUEUE("ENQUEUE"),
		CREATE("CREATE"),
		UPDATE("UPDATE"),
		DELETE("DELETE");

		private final String value;
	}
}
