package com.sofa.linkiving.domain.link.enums;

import com.sofa.linkiving.global.converter.AbstractCodeEnumConverter;
import com.sofa.linkiving.global.converter.CodeEnum;

import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SummaryStatus implements CodeEnum<Integer> {
	PENDING(0), PROCESSING(1), COMPLETED(2), FAILED(3);

	private final Integer code;

	@Converter(autoApply = true)
	static class SummaryStatusConverter extends AbstractCodeEnumConverter<SummaryStatus, Integer> {
		public SummaryStatusConverter() {
			super(SummaryStatus.class);
		}
	}
}
