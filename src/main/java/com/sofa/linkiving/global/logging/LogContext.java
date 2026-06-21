package com.sofa.linkiving.global.logging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.MDC;

public final class LogContext {
	public static final String REQUEST_ID = "requestId";
	public static final String TRACE_ID = "traceId";
	public static final String MEMBER_ID = "memberId";
	public static final String LINK_ID = "linkId";
	public static final String CHAT_ID = "chatId";
	public static final String LOG_CATEGORY = "logCategory";

	private LogContext() {
	}

	public static MdcScope withRequest(String requestId, String traceId) {
		Map<String, String> values = new LinkedHashMap<>();
		putIfHasText(values, REQUEST_ID, requestId);
		putIfHasText(values, TRACE_ID, traceId);
		putIfHasText(values, LOG_CATEGORY, "app");
		return withValues(values);
	}

	public static MdcScope withMemberId(Long memberId) {
		return withValue(MEMBER_ID, memberId);
	}

	public static MdcScope withLinkId(Long linkId) {
		return withValue(LINK_ID, linkId);
	}

	public static MdcScope withChatId(Long chatId) {
		return withValue(CHAT_ID, chatId);
	}

	public static MdcScope withLogCategory(String category) {
		return withValue(LOG_CATEGORY, category);
	}

	public static MdcScope withValue(String key, Object value) {
		Map<String, String> values = new LinkedHashMap<>();
		putIfHasText(values, key, value);
		return withValues(values);
	}

	public static void put(String key, Object value) {
		if (value == null) {
			return;
		}

		String stringValue = String.valueOf(value);
		if (hasText(stringValue)) {
			MDC.put(key, stringValue);
		}
	}

	public static MdcScope withValues(Map<String, String> values) {
		Map<String, String> previous = snapshot();
		values.forEach((key, value) -> {
			if (hasText(value)) {
				MDC.put(key, value);
			}
		});
		return () -> restorePrevious(previous);
	}

	public static Map<String, String> snapshot() {
		Map<String, String> contextMap = MDC.getCopyOfContextMap();
		if (contextMap == null || contextMap.isEmpty()) {
			return Collections.emptyMap();
		}
		return new LinkedHashMap<>(contextMap);
	}

	public static MdcScope restore(Map<String, String> context) {
		Map<String, String> previous = snapshot();
		restoreContext(context);
		return () -> restorePrevious(previous);
	}

	public static void restoreContext(Map<String, String> context) {
		if (context == null || context.isEmpty()) {
			MDC.clear();
			return;
		}
		MDC.setContextMap(context);
	}

	private static void restorePrevious(Map<String, String> previous) {
		restoreContext(previous);
	}

	private static void putIfHasText(Map<String, String> values, String key, Object value) {
		if (value == null) {
			return;
		}

		String stringValue = String.valueOf(value);
		if (hasText(stringValue)) {
			values.put(key, stringValue);
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	@FunctionalInterface
	public interface MdcScope extends AutoCloseable {
		@Override
		void close();
	}
}
