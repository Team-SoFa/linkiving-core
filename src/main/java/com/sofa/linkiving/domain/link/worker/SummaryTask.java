package com.sofa.linkiving.domain.link.worker;

import java.util.Map;

public record SummaryTask(
	Long linkId,
	Map<String, String> logContext
) {
}
