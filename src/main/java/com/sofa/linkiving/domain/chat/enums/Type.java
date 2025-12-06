package com.sofa.linkiving.domain.chat.enums;

import com.sofa.linkiving.global.converter.AbstractCodeEnumConverter;
import com.sofa.linkiving.global.converter.CodeEnum;

import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Type implements CodeEnum<Integer> {
	USER(0), AI(1);

	private final Integer code;

	@Converter(autoApply = true)
	static class TypeConverter extends AbstractCodeEnumConverter<Type, Integer> {
		public TypeConverter() {
			super(Type.class);
		}
	}
}
