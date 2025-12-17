package com.sofa.linkiving.domain.link.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SummaryWorkerProperties 단위 테스트")
class SummaryWorkerPropertiesTest {

	@Test
	@DisplayName("양수 값으로 Properties를 생성할 수 있다")
	void shouldCreatePropertiesWithPositiveValue() {
		// when
		SummaryWorkerProperties properties = new SummaryWorkerProperties(1000);

		// then
		assertThat(properties.sleepMs()).isEqualTo(1000);
	}

	@Test
	@DisplayName("0 이하의 값으로 Properties 생성 시 예외가 발생한다 - 0")
	void shouldThrowExceptionWhenSleepMsIsZero() {
		// when & then
		assertThatThrownBy(() -> new SummaryWorkerProperties(0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("sleepMs must be positive");
	}

	@Test
	@DisplayName("0 이하의 값으로 Properties 생성 시 예외가 발생한다 - 음수")
	void shouldThrowExceptionWhenSleepMsIsNegative() {
		// when & then
		assertThatThrownBy(() -> new SummaryWorkerProperties(-100))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("sleepMs must be positive");
	}

	@Test
	@DisplayName("최소 양수 값으로 Properties를 생성할 수 있다")
	void shouldCreatePropertiesWithMinimumPositiveValue() {
		// when
		SummaryWorkerProperties properties = new SummaryWorkerProperties(1);

		// then
		assertThat(properties.sleepMs()).isEqualTo(1);
	}
}
