package com.sofa.linkiving.global.converter;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LongListToStringConverterTest {
	private final LongListToStringConverter converter = new LongListToStringConverter();

	@Test
	@DisplayName("List<Long>을 JSON 문자열로 변환한다 (convertToDatabaseColumn)")
	void shouldConvertListToJsonString() {
		// given
		List<Long> attribute = List.of(1L, 20L, 300L);

		// when
		String dbData = converter.convertToDatabaseColumn(attribute);

		// then
		assertThat(dbData).isEqualTo("[1,20,300]");
	}

	@Test
	@DisplayName("빈 리스트는 빈 JSON 배열 문자열 '[]'로 변환된다")
	void shouldConvertEmptyListToEmptyJsonArray() {
		// given
		List<Long> attribute = List.of();

		// when
		String dbData = converter.convertToDatabaseColumn(attribute);

		// then
		assertThat(dbData).isEqualTo("[]");
	}

	@Test
	@DisplayName("null 리스트는 빈 JSON 배열 문자열 '[]'로 변환된다 (Null Safe)")
	void shouldConvertNullToEmptyJsonArray() {
		// given
		List<Long> attribute = null;

		// when
		String dbData = converter.convertToDatabaseColumn(attribute);

		// then
		assertThat(dbData).isEqualTo("[]");
	}

	@Test
	@DisplayName("JSON 문자열을 List<Long>으로 변환한다 (convertToEntityAttribute)")
	void shouldConvertJsonStringToList() {
		// given
		String dbData = "[1, 100, 500]";

		// when
		List<Long> attribute = converter.convertToEntityAttribute(dbData);

		// then
		assertThat(attribute).hasSize(3)
			.containsExactly(1L, 100L, 500L);
	}

	@Test
	@DisplayName("빈 JSON 배열 문자열 '[]'은 빈 리스트로 변환된다")
	void shouldConvertEmptyJsonArrayToEmptyList() {
		// given
		String dbData = "[]";

		// when
		List<Long> attribute = converter.convertToEntityAttribute(dbData);

		// then
		assertThat(attribute).isEmpty();
	}

	@Test
	@DisplayName("잘못된 JSON 형식의 문자열 변환 시 IllegalStateException이 발생한다")
	void shouldThrowExceptionWhenJsonIsInvalid() {
		// given
		String invalidJson = "{invalid}";

		// when & then
		assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidJson))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Failed to convert JSON to List<Long>");
	}
}
