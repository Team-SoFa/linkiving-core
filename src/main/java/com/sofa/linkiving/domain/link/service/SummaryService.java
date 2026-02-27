package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.ai.AiSummaryClient;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryService {
	private final SummaryQueryService summaryQueryService;
	private final SummaryCommandService summaryCommandService;
	private final AiSummaryClient aiSummaryClient;

	public String initialSummary(Long linkId, String url, Format format) {
		return aiSummaryClient.generateSummary(linkId, url, format);
	}

	public String comparisonSummary(String existingSummary, String newSummary) {
		return aiSummaryClient.comparisonSummary(existingSummary, newSummary);
	}

	public Summary getSummary(Long linkId) {
		return summaryQueryService.getSummary(linkId);
	}

	public Summary createSummary(Link link, Format format, String content) {
		return summaryCommandService.save(link, format, content);
	}

	public void selectSummary(Long linkId, Long summaryId) {
		summaryCommandService.selectSummary(linkId, summaryId);
	}
}
