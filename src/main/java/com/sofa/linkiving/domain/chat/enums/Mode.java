package com.sofa.linkiving.domain.chat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Mode {
	DETAILED("detailed"),
	CONCISE("concise");

	private final String value;

	@JsonCreator
	public static Mode from(String value) {
		for (Mode mode : Mode.values()) {
			if (mode.getValue().equalsIgnoreCase(value)) {
				return mode;
			}
		}
		return DETAILED;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
