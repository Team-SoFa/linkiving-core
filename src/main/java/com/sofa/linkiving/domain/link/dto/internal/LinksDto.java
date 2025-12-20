package com.sofa.linkiving.domain.link.dto.internal;

import java.util.List;

public record LinksDto(
	List<LinkDto> linkDtos,
	boolean hasNext
) {
}

