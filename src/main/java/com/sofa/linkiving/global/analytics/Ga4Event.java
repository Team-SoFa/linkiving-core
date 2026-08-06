package com.sofa.linkiving.global.analytics;

import java.util.Map;

public record Ga4Event(
	String name,
	Map<String, Object> params
) {
	public Ga4Event {
		params = params == null ? Map.of() : Map.copyOf(params);
	}
}
