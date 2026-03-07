package com.sofa.linkiving.domain.link.event;

import com.sofa.linkiving.domain.link.dto.response.SummaryStatusRes;

public record SummaryStatusEvent(
	String email,
	SummaryStatusRes response
) {
}
