package com.sofa.linkiving.domain.link.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SummaryWorkerFacade {

	private final LinkService linkService;
	private final SummaryService summaryService;

	@Transactional(readOnly = true)
	public Link getLinkWithMember(Long linkId) {
		return linkService.getLinkWithMember(linkId);
	}

	@Transactional
	public void updateSummaryStatus(Long linkId, SummaryStatus status) {
		linkService.updateSummaryStatus(linkId, status);
	}

	@Transactional
	public Summary createInitialSummaryAndUpdateStatus(Long linkId, String summaryContent) {
		Link link = linkService.getLink(linkId);

		if (link.getSummaryStatus() != SummaryStatus.PROCESSING) {
			log.warn("요약 상태가 PROCESSING이 아니므로 초기 요약 저장을 무시함 - linkId: {}", linkId);
			return null;
		}

		Summary savedSummary = summaryService.createInitialSummary(link, summaryContent);

		link.updateSummaryStatus(SummaryStatus.COMPLETED);

		return savedSummary;
	}
}
