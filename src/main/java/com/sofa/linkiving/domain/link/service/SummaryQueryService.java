package com.sofa.linkiving.domain.link.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryQueryService {
	private final SummaryRepository summaryRepository;

	public Summary getSummary(Long linkId) {
		return summaryRepository.findByLinkIdAndSelectedTrue(linkId).orElseThrow(
			() -> new BusinessException(LinkErrorCode.SUMMARY_NOT_FOUND)
		);
	}

	public Map<Long, Summary> getSelectedSummariesByLinks(List<Link> links) {
		if (links.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Summary> summaries = summaryRepository.findAllByLinkInAndSelectedTrue(links);

		return summaries.stream()
			.collect(Collectors.toMap(
				s -> s.getLink().getId(),
				s -> s,
				(existing, replacement) -> existing
			));
	}
}
