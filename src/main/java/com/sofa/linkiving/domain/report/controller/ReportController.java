package com.sofa.linkiving.domain.report.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.report.dto.request.ReportReq;
import com.sofa.linkiving.domain.report.service.ReportService;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.security.annotation.AuthMember;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/report")
@RequiredArgsConstructor
public class ReportController implements ReportApi {
	private final ReportService reportService;

	@Override
	@PostMapping
	public BaseResponse<String> createReport(@AuthMember Member member, @RequestBody ReportReq res) {
		reportService.create(member, res.content());
		return BaseResponse.noContent("제보가 성공적으로 접수되었습니다.");
	}
}
