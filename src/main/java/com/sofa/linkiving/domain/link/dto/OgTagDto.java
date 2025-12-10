package com.sofa.linkiving.domain.link.dto;

import lombok.Builder;

@Builder
public record OgTagDto(
	String title,
	String description,
	String image,
	String url
) {
	public static OgTagDto empty() {
		return OgTagDto.builder().build();
	}
}
