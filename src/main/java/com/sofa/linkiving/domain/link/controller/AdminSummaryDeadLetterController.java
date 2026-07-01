package com.sofa.linkiving.domain.link.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.link.dto.response.SummaryDeadLetterRes;
import com.sofa.linkiving.domain.link.enums.DeadLetterStatus;
import com.sofa.linkiving.domain.link.service.SummaryDeadLetterService;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.global.config.annotation.DecodeHash;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/admin/summary/dead-letters")
@RequiredArgsConstructor
public class AdminSummaryDeadLetterController {

	private final SummaryDeadLetterService summaryDeadLetterService;

	@GetMapping
	public BaseResponse<Page<SummaryDeadLetterRes>> getDeadLetters(
		@RequestParam(required = false) DeadLetterStatus status,
		@PageableDefault(size = 20) Pageable pageable) {
		Page<SummaryDeadLetterRes> result = summaryDeadLetterService.getDeadLetters(status, pageable)
			.map(SummaryDeadLetterRes::from);
		return BaseResponse.success(result, "데드레터 목록을 조회했습니다.");
	}

	@PostMapping("/{id}/reprocess")
	public BaseResponse<String> reprocess(@PathVariable @DecodeHash Long id) {
		summaryDeadLetterService.reprocess(id);
		return BaseResponse.noContent("데드레터 재처리를 요청했습니다.");
	}

	@PostMapping("/{id}/ignore")
	public BaseResponse<String> ignore(@PathVariable @DecodeHash Long id) {
		summaryDeadLetterService.ignore(id);
		return BaseResponse.noContent("데드레터를 무시 처리했습니다.");
	}
}
