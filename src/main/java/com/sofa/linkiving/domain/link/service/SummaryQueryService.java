package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;

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
		return summaryRepository.findById(linkId).orElseThrow(
			() -> new BusinessException(LinkErrorCode.SUMMARY_NOT_FOUND)
		);
	}
}
