package com.sofa.linkiving.domain.chat.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.chat.dto.request.UpsertFeedbackReq;
import com.sofa.linkiving.domain.chat.dto.response.UpsertFeedbackRes;
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
	@PutMapping("/{messageId}/feedback")
	public BaseResponse<UpsertFeedbackRes> upsertFeedback(
		@PathVariable Long messageId,
		@Valid @RequestBody UpsertFeedbackReq req,
		@AuthMember Member member
	) {
		UpsertFeedbackRes res = feedbackFacade.upsertFeedback(member, messageId, req.sentiment(), req.text());
		return BaseResponse.success(res, "피드백이 등록되었습니다.");
	}
}
