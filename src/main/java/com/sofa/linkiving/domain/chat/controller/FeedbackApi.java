package com.sofa.linkiving.domain.chat.controller;

import com.sofa.linkiving.domain.chat.dto.request.UpsertFeedbackReq;
import com.sofa.linkiving.domain.chat.dto.response.UpsertFeedbackRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Feedback", description = "피드백 관리 API")
public interface FeedbackApi {
	@Operation(summary = "피드백 추가 및 수정", description = "메세지에 피드백을 추가 및 수정하고 피드백 ID를 반환합니다.")
	BaseResponse<UpsertFeedbackRes> upsertFeedback(Long messageId, UpsertFeedbackReq createFeedbackReq, Member member);
}
