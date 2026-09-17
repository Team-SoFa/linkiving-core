package com.sofa.linkiving.domain.chat.dto.internal;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;

public record RagHistoryLinkRow(Long messageId, Link link, Summary summary) {
}
