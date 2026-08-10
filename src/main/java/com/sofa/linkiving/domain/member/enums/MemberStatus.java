package com.sofa.linkiving.domain.member.enums;

import com.sofa.linkiving.global.converter.AbstractCodeEnumConverter;
import com.sofa.linkiving.global.converter.CodeEnum;

import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberStatus implements CodeEnum<Integer> {

	PENDING_TERMS(1), ACTIVE(2), WITHDRAWING(3), WITHDRAWAL_ANALYTICS_SENT(4);

	private final Integer code;

	@Converter(autoApply = true)
	static class MemberStatusConverter extends AbstractCodeEnumConverter<MemberStatus, Integer> {
		public MemberStatusConverter() {
			super(MemberStatus.class);
		}
	}
}
