package com.sofa.linkiving.domain.link.event;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.SyncAction;

public record LinkSyncEvent(
	LinkSyncUpdateReq req,
	SyncAction action
) {
	public static LinkSyncEvent deleteEvent(Long linkId) {
		LinkSyncUpdateReq req = LinkSyncUpdateReq.builder()
			.linkId(linkId)
			.build();
		return new LinkSyncEvent(req, SyncAction.DELETE);
	}

	public static LinkSyncEvent createEvent(Link link) {
		LinkSyncUpdateReq req = LinkSyncUpdateReq.from(link);
		return new LinkSyncEvent(req, SyncAction.CREATE);
	}

	public static LinkSyncEvent updateEvent(Link link, Summary summary) {
		LinkSyncUpdateReq req = LinkSyncUpdateReq.of(link, summary);
		return new LinkSyncEvent(req, SyncAction.UPDATE);
	}
}
