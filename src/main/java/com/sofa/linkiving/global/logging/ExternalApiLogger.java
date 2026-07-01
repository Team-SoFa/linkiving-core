package com.sofa.linkiving.global.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExternalApiLogger {

	private static final Logger LOG = LoggerFactory.getLogger("EXTERNAL_API");
	private static final String CATEGORY = "external";
	private static final int MAX_REASON_LENGTH = 300;

	private ExternalApiLogger() {
	}

	public static Entry client(String client, String operation) {
		return new Entry(client, operation);
	}

	private static String sanitize(String value) {
		if (value == null) {
			return null;
		}
		String cleaned = value.replaceAll("\\s+", " ").trim();
		if (cleaned.length() > MAX_REASON_LENGTH) {
			cleaned = cleaned.substring(0, MAX_REASON_LENGTH) + "...";
		}
		return cleaned;
	}

	public static final class Entry {
		private final String client;
		private final String operation;
		private final StringBuilder details = new StringBuilder();
		private long elapsedMs = -1L;
		private String errorCode;
		private String exceptionType;
		private String reason;

		private Entry(String client, String operation) {
			this.client = client;
			this.operation = operation;
		}

		public Entry elapsedMs(long elapsedMs) {
			this.elapsedMs = elapsedMs;
			return this;
		}

		public Entry errorCode(String errorCode) {
			this.errorCode = errorCode;
			return this;
		}

		public Entry cause(Throwable throwable) {
			if (throwable != null) {
				this.exceptionType = throwable.getClass().getSimpleName();
				this.reason = sanitize(throwable.getMessage());
			}
			return this;
		}

		public Entry detail(String key, Object value) {
			if (value != null) {
				details.append(' ').append(key).append('=').append(sanitize(String.valueOf(value)));
			}
			return this;
		}

		public void failure() {
			write("FAILURE");
		}

		public void empty() {
			write("EMPTY");
		}

		private void write(String outcome) {
			String message = build(outcome);
			try (LogContext.MdcScope ignored = LogContext.withLogCategory(CATEGORY)) {
				LOG.warn(message);
			}
		}

		private String build(String outcome) {
			StringBuilder sb = new StringBuilder(64);
			sb.append("outcome=").append(outcome)
				.append(" client=").append(client)
				.append(" operation=").append(operation);
			if (!details.isEmpty()) {
				sb.append(details);
			}
			if (errorCode != null) {
				sb.append(" code=").append(errorCode);
			}
			if (exceptionType != null) {
				sb.append(" exception=").append(exceptionType);
			}
			if (elapsedMs >= 0L) {
				sb.append(" elapsedMs=").append(elapsedMs);
			}
			if (reason != null) {
				sb.append(" reason=\"").append(reason).append('"');
			}
			return sb.toString();
		}
	}
}
