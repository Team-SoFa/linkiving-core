package com.sofa.linkiving.domain.link.enums;

import com.sofa.linkiving.global.converter.AbstractCodeEnumConverter;
import com.sofa.linkiving.global.converter.CodeEnum;

import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeadLetterStatus implements CodeEnum<Integer> {
	PENDING(0), REPROCESSED(1), IGNORED(2);

	private final Integer code;

	@Converter(autoApply = true)
	static class DeadLetterStatusConverter extends AbstractCodeEnumConverter<DeadLetterStatus, Integer> {
		public DeadLetterStatusConverter() {
			super(DeadLetterStatus.class);
		}
	}
}
