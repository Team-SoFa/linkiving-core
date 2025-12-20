package com.sofa.linkiving.domain.link.dto.internal;

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
