package com.sofa.linkiving.global.metrics;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * 동일 메트릭 이름은 동일 태그 키셋을 가져야 한다.
 * 키셋이 갈리면 등록 충돌·집계 오류가 발생하므로, 팩토리를 통해 등록된 모든 조합의 키셋 일관성을 검증한다.
 */
class MetricsTagConsistencyTest {

	private MeterRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new SimpleMeterRegistry();
	}

	private Set<Set<String>> tagKeySetsOf(String metricName) {
		return registry.getMeters().stream()
			.filter(meter -> meter.getId().getName().equals(metricName))
			.map(meter -> meter.getId().getTags().stream()
				.map(io.micrometer.core.instrument.Tag::getKey)
				.collect(Collectors.toSet()))
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("ai.client.calls: 모든 조합이 client·operation·result 키셋으로 등록된다")
	void aiClientCalls_hasConsistentTagKeySet() {
		for (AiClientMetrics.Client client : AiClientMetrics.Client.values()) {
			for (AiClientMetrics.Operation operation : AiClientMetrics.Operation.values()) {
				for (AiClientMetrics.Result result : AiClientMetrics.Result.values()) {
					AiClientMetrics.counter(registry, client, operation, result);
				}
			}
		}

		Set<Set<String>> keySets = tagKeySetsOf("ai.client.calls");

		assertThat(keySets).hasSize(1);
		assertThat(keySets.iterator().next())
			.containsExactlyInAnyOrder("client", "operation", "result");
	}

	@Test
	@DisplayName("async.task.failures: 모든 조합이 task·action 키셋으로 등록된다")
	void asyncTaskFailures_hasConsistentTagKeySet() {
		for (AsyncTaskMetrics.Task task : AsyncTaskMetrics.Task.values()) {
			for (AsyncTaskMetrics.Action action : AsyncTaskMetrics.Action.values()) {
				AsyncTaskMetrics.failureCounter(registry, task, action);
			}
		}

		Set<Set<String>> keySets = tagKeySetsOf("async.task.failures");

		assertThat(keySets).hasSize(1);
		assertThat(keySets.iterator().next())
			.containsExactlyInAnyOrder("task", "action");
	}

	@Test
	@DisplayName("동일 태그 조합으로 재등록하면 같은 미터를 반환한다 (중복 등록 없음)")
	void sameTags_returnSameMeter() {
		var first = AiClientMetrics.counter(registry, AiClientMetrics.Client.SUMMARY,
			AiClientMetrics.Operation.INITIAL, AiClientMetrics.Result.SUCCESS);
		var second = AiClientMetrics.counter(registry, AiClientMetrics.Client.SUMMARY,
			AiClientMetrics.Operation.INITIAL, AiClientMetrics.Result.SUCCESS);

		assertThat(first).isSameAs(second);
		assertThat(registry.getMeters().stream()
			.filter(meter -> meter.getId().getName().equals("ai.client.calls"))
			.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("등록된 미터는 모두 Counter 타입이다")
	void registeredMeters_areCounters() {
		AiClientMetrics.counter(registry, AiClientMetrics.Client.ANSWER,
			AiClientMetrics.Operation.GENERATE, AiClientMetrics.Result.FAILURE);
		AsyncTaskMetrics.failureCounter(registry, AsyncTaskMetrics.Task.SUMMARY,
			AsyncTaskMetrics.Action.GENERATE);

		assertThat(registry.getMeters())
			.allSatisfy(meter -> assertThat(meter.getId().getType()).isEqualTo(Meter.Type.COUNTER));
	}
}
