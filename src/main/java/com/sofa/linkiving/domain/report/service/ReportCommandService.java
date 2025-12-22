package com.sofa.linkiving.domain.report.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.report.entity.Report;
import com.sofa.linkiving.domain.report.repository.ReportRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportCommandService {
	private final ReportRepository reportRepository;

	public Report save(Member member, String content) {
		Report report = Report.builder()
			.member(member)
			.content(content)
			.build();

		return reportRepository.save(report);
	}
}
