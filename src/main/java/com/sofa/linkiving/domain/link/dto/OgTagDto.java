package com.sofa.linkiving.domain.link.dto;

import lombok.Builder;

@Builder
public record OgTagDto(
	String title,
	String description,
	String image,
	String url
) {
	public static final OgTagDto EMPTY = new OgTagDto("", "", "", "");
}
