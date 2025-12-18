package com.sofa.linkiving.domain.chat.controller;

import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Chat", description = "채팅 관리 API")
public interface ChatApi {
	@Operation(summary = "채팅방 목록 조회", description = "사용자의 채팅방 목록 정보(채팅방 Id, 제목)을 조회합니다.")
	BaseResponse<ChatsRes> getChats(Member member);
}
