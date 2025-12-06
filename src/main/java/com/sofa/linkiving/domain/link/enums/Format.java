package com.sofa.linkiving.domain.link.enums;

import com.sofa.linkiving.global.converter.AbstractCodeEnumConverter;
import com.sofa.linkiving.global.converter.CodeEnum;

import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Format implements CodeEnum<Integer> {
	CONCISE(1), DETAILED(2);
	private final Integer code;

	@Converter(autoApply = true)
	static class FormatConverter extends AbstractCodeEnumConverter<Format, Integer> {
		public FormatConverter() {
			super(Format.class);
		}
	}
}
