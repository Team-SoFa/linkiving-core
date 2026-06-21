package com.sofa.linkiving.global.logging;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcTaskDecoratorTest {

	private final MdcTaskDecorator decorator = new MdcTaskDecorator();

	@AfterEach
	void tearDown() {
		MDC.clear();
	}

	@Test
	@DisplayName("비동기 작업 데코레이터는 요청 MDC를 그대로 전파한다")
	void shouldPropagateMdcContext() {
		MDC.put(LogContext.REQUEST_ID, "req-123");
		MDC.put(LogContext.TRACE_ID, "trace-456");
		MDC.put(LogContext.MEMBER_ID, "99");

		AtomicReference<Map<String, String>> captured = new AtomicReference<>();
		Runnable decorated = decorator.decorate(() -> captured.set(LogContext.snapshot()));

		MDC.clear();
		decorated.run();

		assertThat(captured.get())
			.containsEntry(LogContext.REQUEST_ID, "req-123")
			.containsEntry(LogContext.TRACE_ID, "trace-456")
			.containsEntry(LogContext.MEMBER_ID, "99");
		assertThat(MDC.getCopyOfContextMap()).isNull();
	}
}
