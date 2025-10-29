package com.sofa.linkiving.global.converter;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AbstractCodeEnumConverterTest {
	private final TestEnum.TestEnumConverter converter = new TestEnum.TestEnumConverter();

	@Test
	@DisplayName("Enum을 Code로 변환")
	void shouldConvertEnumToCode() {
		assertThat(converter.convertToDatabaseColumn(TestEnum.A)).isEqualTo(1);
		assertThat(converter.convertToDatabaseColumn(TestEnum.C)).isEqualTo(9);
	}

	@Test
	@DisplayName("Code 값을 Enum으로 변환")
	void shouldConvertCodeToEnum() {
		assertThat(converter.convertToEntityAttribute(1)).isEqualTo(TestEnum.A);
		assertThat(converter.convertToEntityAttribute(2)).isEqualTo(TestEnum.B);
		assertThat(converter.convertToEntityAttribute(9)).isEqualTo(TestEnum.C);
	}

	@Test
	@DisplayName("null 입력 시에도 예외 없이 정상 동작")
	void shouldHandleNulls() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
		assertThat(converter.convertToEntityAttribute(null)).isNull();
	}

	@Test
	@DisplayName("알 수 없는 코드 입력 시 예외 발생")
	void shouldThrowForUnknownCode() {
		assertThatThrownBy(() -> converter.convertToEntityAttribute(999))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Unknown code");
	}
}
