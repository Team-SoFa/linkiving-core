package com.sofa.linkiving.global.analytics;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

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

	@Test
	void debugMode_bindsFromEnvironmentVariable() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
			"env", Map.of("ANALYTICS_GA4_DEBUG_MODE", "true")));

		Ga4Properties properties = Binder.get(environment)
			.bind("analytics.ga4", Bindable.of(Ga4Properties.class))
			.orElseThrow(() -> new IllegalStateException("GA4 properties binding failed"));

		assertThat(properties.debugMode()).isTrue();
	}

	private Ga4Properties bind(Map<String, String> values) {
		return new Binder(new MapConfigurationPropertySource(values))
			.bind("analytics.ga4", Bindable.of(Ga4Properties.class))
			.orElseThrow(() -> new IllegalStateException("GA4 properties binding failed"));
	}
}
