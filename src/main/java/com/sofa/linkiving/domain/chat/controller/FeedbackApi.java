package com.sofa.linkiving.domain.chat.controller;

import com.sofa.linkiving.domain.chat.dto.request.AddFeedbackReq;
import com.sofa.linkiving.domain.chat.dto.response.AddFeedbackRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Feedback", description = "피드백 관리 API")
public interface FeedbackApi {
	@Operation(summary = "피드백 추가", description = "메세지에 피드백을 추가하고 생성된 피드백 ID를 반환합니다.")
	BaseResponse<AddFeedbackRes> createFeedback(Long messageId, AddFeedbackReq createFeedbackReq, Member member);
}
