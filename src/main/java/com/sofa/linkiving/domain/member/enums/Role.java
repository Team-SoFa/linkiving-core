package com.sofa.linkiving.domain.member.enums;

import com.sofa.linkiving.global.converter.AbstractCodeEnumConverter;
import com.sofa.linkiving.global.converter.CodeEnum;

import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role implements CodeEnum<Integer> {

	USER(1), ADMIN(2);

	private final Integer code;

	@Converter(autoApply = true)
	static class RoleConverter extends AbstractCodeEnumConverter<Role, Integer> {
		public RoleConverter() {
			super(Role.class);
		}
	}
}
