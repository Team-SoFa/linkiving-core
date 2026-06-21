package com.sofa.linkiving.global.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AccessLogger {
	private static final Logger LOG = LoggerFactory.getLogger("ACCESS");

	private AccessLogger() {
	}

	public static void info(String message, Object... arguments) {
		try (LogContext.MdcScope ignored = LogContext.withLogCategory("access")) {
			LOG.info(message, arguments);
		}
	}
}
