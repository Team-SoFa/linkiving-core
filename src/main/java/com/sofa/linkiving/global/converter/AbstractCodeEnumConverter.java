package com.sofa.linkiving.global.converter;

import java.util.Arrays;
import java.util.Objects;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public abstract class AbstractCodeEnumConverter<T extends Enum<T> & CodeEnum<E>, E>
	implements AttributeConverter<T, E> {
	private final Class<T> data;

	protected AbstractCodeEnumConverter(Class<T> data) {
		this.data = data;
	}

	@Override
	public E convertToDatabaseColumn(T attribute) {
		return (attribute == null) ? null : attribute.getCode();
	}

	@Override
	public T convertToEntityAttribute(E code) {
		if (Objects.isNull(code)) {
			return null;
		}
		return Arrays.stream(data.getEnumConstants())
			.filter(e -> e.getCode().equals(code))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown code: " + code));
	}
}
