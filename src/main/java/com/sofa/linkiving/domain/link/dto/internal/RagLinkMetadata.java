package com.sofa.linkiving.domain.link.dto.internal;

import java.time.ZoneId;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;

public record RagLinkMetadata(String savedAt, Long savedAtEpochMs, SummaryStatus summaryStatus) {

	// Persisted audit timestamps are Korean local time, not the container's UTC clock.
	private static final ZoneId AUDIT_ZONE = ZoneId.of("Asia/Seoul");

	public static RagLinkMetadata from(Link link) {
		if (link.getCreatedAt() == null) {
			return new RagLinkMetadata(null, null, link.getSummaryStatus());
		}
		var savedAt = link.getCreatedAt().atZone(AUDIT_ZONE).toOffsetDateTime();
		return new RagLinkMetadata(savedAt.toString(), savedAt.toInstant().toEpochMilli(), link.getSummaryStatus());
	}
}
