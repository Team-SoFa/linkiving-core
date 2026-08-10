package com.sofa.linkiving.global.analytics;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class Ga4PropertiesTest {

	@Test
	void debugMode_defaultsToFalseWhenPropertyIsMissing() {
		Ga4Properties properties = bind(Map.of("analytics.ga4.enabled", "true"));

		assertThat(properties.debugMode()).isFalse();
	}

	@Test
	void debugMode_bindsFromConfigurationProperty() {
		Ga4Properties properties = bind(Map.of("analytics.ga4.debug-mode", "true"));

		assertThat(properties.debugMode()).isTrue();
	}

	private Ga4Properties bind(Map<String, String> values) {
		return new Binder(new MapConfigurationPropertySource(values))
			.bind("analytics.ga4", Bindable.of(Ga4Properties.class))
			.orElseThrow();
	}
}
