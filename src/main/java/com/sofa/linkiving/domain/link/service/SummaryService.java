package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SummaryService {
	private final SummaryQueryService summaryQueryService;
	private final SummaryCommandService summaryCommandService;

	@Transactional(readOnly = true)
	public Summary getSummary(Long linkId) {
		return summaryQueryService.getSummary(linkId);
	}

	public Summary createSummary(Link link, Format format, String summary) {
		return summaryCommandService.save(link, format, summary);
	}
}
