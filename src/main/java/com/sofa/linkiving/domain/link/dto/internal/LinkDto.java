package com.sofa.linkiving.domain.link.dto.internal;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;

public record LinkDto(
	Link link,
	Summary summary
) {
}
