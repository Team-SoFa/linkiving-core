package com.sofa.linkiving.global.config.jackson;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sofa.linkiving.global.util.HashidsUtils;

@Component
public class HashidsSerializer extends JsonSerializer<Long> {
	private static HashidsUtils hashidsUtils;

	@Autowired
	public void setHashidsUtils(HashidsUtils hashidsUtils) {
		HashidsSerializer.hashidsUtils = hashidsUtils;
	}

	@Override
	public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else {
			gen.writeString(hashidsUtils.encode(value));
		}
	}
}
