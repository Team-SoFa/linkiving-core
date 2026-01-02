package com.sofa.linkiving.domain.report.controller;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.report.dto.request.ReportReq;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Report", description = "오류 및 버그 제보 관리 API")
public interface ReportApi {
	BaseResponse<String> createReport(Member member, @Valid ReportReq res);
}
