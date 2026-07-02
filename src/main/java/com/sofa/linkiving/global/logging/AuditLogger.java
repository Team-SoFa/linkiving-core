package com.sofa.linkiving.global.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuditLogger {
	private static final Logger LOG = LoggerFactory.getLogger("AUDIT");

	private AuditLogger() {
	}

	public static void info(String message, Object... arguments) {
		try (LogContext.MdcScope ignored = LogContext.withLogCategory("audit")) {
			LOG.info(message, arguments);
		}
	}

	public static void warn(String message, Object... arguments) {
		try (LogContext.MdcScope ignored = LogContext.withLogCategory("audit")) {
			LOG.warn(message, arguments);
		}
	}
}
