package com.sofa.linkiving.domain.chat.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.chat.dto.request.AddFeedbackReq;
import com.sofa.linkiving.domain.chat.dto.response.AddFeedbackRes;
import com.sofa.linkiving.domain.chat.facade.FeedbackFacade;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.security.annotation.AuthMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
public class FeedbackController implements FeedbackApi {
	private final FeedbackFacade feedbackFacade;

	@Override
	@PostMapping("/{messageId}/feedback")
	public BaseResponse<AddFeedbackRes> createFeedback(@PathVariable Long messageId,
		@Valid @RequestBody AddFeedbackReq req, @AuthMember Member member) {
		AddFeedbackRes res = feedbackFacade.createFeedback(member, messageId, req.sentiment(), req.text());
		return BaseResponse.success(res, "피드백이 등록되었습니다.");
	}
}
