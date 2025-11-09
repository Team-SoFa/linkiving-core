package com.sofa.linkiving.global.converter;

import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TestEnum implements CodeEnum<Integer> {
	A(1), B(2), C(9);
	private final Integer code;

	@Converter(autoApply = true)
	static class TestEnumConverter extends AbstractCodeEnumConverter<TestEnum, Integer> {
		public TestEnumConverter() {
			super(TestEnum.class);
		}
	}
}
