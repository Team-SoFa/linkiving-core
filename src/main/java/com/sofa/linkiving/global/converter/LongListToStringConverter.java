package com.sofa.linkiving.global.converter;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LongListToStringConverter implements AttributeConverter<List<Long>, String> {
	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<Long> attribute) {
		try {
			return (attribute == null || attribute.isEmpty()) ? "[]" : mapper.writeValueAsString(attribute);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to convert List<Long> to JSON", e);
		}
	}

	@Override
	public List<Long> convertToEntityAttribute(String dbData) {
		try {
			return mapper.readValue(dbData, new TypeReference<List<Long>>() {
			});
		} catch (Exception e) {
			throw new IllegalStateException("Failed to convert JSON to List<Long>", e);
		}
	}
}
