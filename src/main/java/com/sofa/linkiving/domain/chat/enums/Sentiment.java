package com.sofa.linkiving.domain.chat.enums;

import com.sofa.linkiving.global.converter.AbstractCodeEnumConverter;
import com.sofa.linkiving.global.converter.CodeEnum;

import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Sentiment implements CodeEnum<Integer> {
	LIKE(0), DISLIKE(1);
	private final Integer code;

	@Converter(autoApply = true)
	static class SentimentConverter extends AbstractCodeEnumConverter<Sentiment, Integer> {
		public SentimentConverter() {
			super(Sentiment.class);
		}
	}
}
