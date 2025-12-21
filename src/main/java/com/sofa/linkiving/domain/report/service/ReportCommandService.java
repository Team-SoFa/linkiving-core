package com.sofa.linkiving.domain.report.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.report.repository.ReportRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportCommandService {
	private final ReportRepository reportRepository;
}
