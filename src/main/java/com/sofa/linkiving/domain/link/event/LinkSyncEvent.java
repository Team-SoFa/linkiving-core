package com.sofa.linkiving.domain.link.event;

import java.util.Map;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.SyncAction;
import com.sofa.linkiving.global.logging.LogContext;

public record LinkSyncEvent(
	LinkSyncUpdateReq req,
	SyncAction action,
	Map<String, String> logContext
) {
	public LinkSyncEvent(LinkSyncUpdateReq req, SyncAction action) {
		this(req, action, Map.of());
	}

	public static LinkSyncEvent deleteEvent(Long linkId) {
		LinkSyncUpdateReq req = LinkSyncUpdateReq.builder()
			.linkId(linkId)
			.build();
		return new LinkSyncEvent(req, SyncAction.DELETE, LogContext.snapshot());
	}

	public static LinkSyncEvent createEvent(Link link) {
		LinkSyncUpdateReq req = LinkSyncUpdateReq.from(link);
		return new LinkSyncEvent(req, SyncAction.CREATE, LogContext.snapshot());
	}

	public static LinkSyncEvent updateEvent(Link link, Summary summary) {
		LinkSyncUpdateReq req = LinkSyncUpdateReq.of(link, summary);
		return new LinkSyncEvent(req, SyncAction.UPDATE, LogContext.snapshot());
	}
}
