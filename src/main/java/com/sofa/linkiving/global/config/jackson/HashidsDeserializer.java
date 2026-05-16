package com.sofa.linkiving.global.config.jackson;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sofa.linkiving.global.util.HashidsUtils;

@Component
public class HashidsDeserializer extends JsonDeserializer<Long> {

	private static HashidsUtils hashidsUtils;

	@Autowired
	public void setHashidsUtils(HashidsUtils hashidsUtils) {
		HashidsDeserializer.hashidsUtils = hashidsUtils;
	}

	@Override
	public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		String hash = parser.getValueAsString();
		if (hash == null || hash.isBlank()) {
			return null;
		}
		return hashidsUtils.decode(hash);
	}
}
