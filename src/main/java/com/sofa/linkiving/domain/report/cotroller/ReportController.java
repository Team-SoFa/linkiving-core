package com.sofa.linkiving.domain.report.cotroller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.report.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/report")
@RequiredArgsConstructor
public class ReportController implements ReportApi {
	private final ReportService reportService;
}
