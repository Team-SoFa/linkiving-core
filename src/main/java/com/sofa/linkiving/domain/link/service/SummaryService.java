package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryService {
	private final SummaryQueryService summaryQueryService;
	private final SummaryCommandService summaryCommandService;

	public Summary getSummary(Long linkId) {
		return summaryQueryService.getSummary(linkId);
	}

	public Summary createSummary(Link link, Format format, String summary) {
		return summaryCommandService.save(link, format, summary);
	}

	public Summary createInitialSummary(Link link, String summary) {
		return summaryCommandService.initialSave(link, Format.CONCISE, summary);
	}

	public void selectSummary(Long linkId, Long summaryId) {
		summaryCommandService.selectSummary(linkId, summaryId);
	}
}
