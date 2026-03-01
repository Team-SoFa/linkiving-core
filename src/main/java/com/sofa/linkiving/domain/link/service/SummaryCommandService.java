package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryCommandService {

	private final SummaryRepository summaryRepository;

	/**
	 * 특정 링크에서 선택된 요약을 변경한다. (링크당 selected=true는 최대 1개)
	 */
	public void selectSummary(Long linkId, Long summaryId) {
		summaryRepository.clearSelectedByLinkId(linkId);
		int updated = summaryRepository.selectByIdAndLinkId(summaryId, linkId);
		if (updated == 0) {
			throw new BusinessException(LinkErrorCode.SUMMARY_NOT_FOUND);
		}
	}

	public Summary save(Link link, Format format, String content) {
		return summaryRepository.save(
			Summary.builder()
				.link(link)
				.format(format)
				.content(content)
				.build()
		);
	}
}

