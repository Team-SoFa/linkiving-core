package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryCommandService {
	private final SummaryRepository summaryRepository;

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
