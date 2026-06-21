package com.sofa.linkiving.global.logging;

import java.util.Map;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

@Component
public class MdcTaskDecorator implements TaskDecorator {

	@Override
	public Runnable decorate(Runnable runnable) {
		Map<String, String> context = LogContext.snapshot();
		return () -> {
			try (LogContext.MdcScope ignored = LogContext.restore(context)) {
				runnable.run();
			}
		};
	}
}
