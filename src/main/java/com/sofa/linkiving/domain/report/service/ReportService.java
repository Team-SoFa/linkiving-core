package com.sofa.linkiving.domain.report.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportService {
	private final ReportCommandService reportCommandService;

	public void create(Member member, String content) {
		reportCommandService.save(member, content);
	}
}
