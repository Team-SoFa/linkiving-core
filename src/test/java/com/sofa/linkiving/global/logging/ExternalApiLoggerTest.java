package com.sofa.linkiving.global.logging;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class ExternalApiLoggerTest {

	private Logger logger;
	private ListAppender<ILoggingEvent> appender;

	@BeforeEach
	void setUp() {
		logger = (Logger)LoggerFactory.getLogger("EXTERNAL_API");
		appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
	}

	@AfterEach
	void tearDown() {
		logger.detachAppender(appender);
	}

	private String lastMessage() {
		return appender.list.get(appender.list.size() - 1).getFormattedMessage();
	}

	@Test
	@DisplayName("failure: 모든 필드가 key=value 로 기록된다")
	void failure_withAllFields() {
		ExternalApiLogger.client("summary", "initialSummary")
			.detail("linkId", 42L)
			.elapsedMs(123L)
			.errorCode("E_000")
			.cause(new RuntimeException("boom"))
			.failure();

		assertThat(lastMessage())
			.contains("outcome=FAILURE")
			.contains("client=summary")
			.contains("operation=initialSummary")
			.contains("linkId=42")
			.contains("code=E_000")
			.contains("exception=RuntimeException")
			.contains("elapsedMs=123")
			.contains("reason=\"boom\"");
	}

	@Test
	@DisplayName("empty: outcome=EMPTY 로 기록되고 code 는 없다")
	void empty_outcome() {
		ExternalApiLogger.client("answer", "generateAnswer")
			.elapsedMs(10L)
			.empty();

		assertThat(lastMessage())
			.contains("outcome=EMPTY")
			.contains("client=answer")
			.contains("operation=generateAnswer")
			.contains("elapsedMs=10")
			.doesNotContain("code=");
	}

	@Test
	@DisplayName("cause 메시지가 null 이면 reason 을 남기지 않는다")
	void cause_nullMessage_omitsReason() {
		ExternalApiLogger.client("summary", "op")
			.cause(new RuntimeException())
			.failure();

		assertThat(lastMessage())
			.contains("exception=RuntimeException")
			.doesNotContain("reason=");
	}

	@Test
	@DisplayName("긴 reason 은 잘려서 ... 이 붙는다")
	void cause_longMessage_truncated() {
		String longMessage = "x".repeat(500);

		ExternalApiLogger.client("summary", "op")
			.cause(new RuntimeException(longMessage))
			.failure();

		assertThat(lastMessage()).contains("...");
	}
}
