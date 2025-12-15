package com.sofa.linkiving.domain.chat.controller;

import com.sofa.linkiving.domain.chat.dto.request.CreateChatReq;
import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.chat.dto.response.CreateChatRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Chat", description = """
	AI 채팅 통합 명세 (HTTP + WebSocket)

	### 📡 1. WebSocket 연결 정보 (필수)
		답변을 실시간으로 수신하기 위해 **반드시 소켓 연결 및 구독**이 선행되어야 합니다.

		* **Socket Endpoint:** `ws://{domain}/ws/chat`
		* **Subscribe Path:** `/topic/chat/{chatId}`
		* **Auth Header:** `Authorization: Bearer {accessToken}` (CONNECT 프레임 헤더)
	### 🚀 2. 동작 흐름
		1. **소켓 연결:** 프론트엔드에서 WebSocket 연결 및 `/topic/chat/{chatId}` 구독
		2. **질문 전송:** `/app/send/{chatId}` (STOMP)로 질문 전송
		3. **답변 수신:** 소켓 구독 채널로 토큰 단위 답변 스트리밍 (`String` 데이터)
		4. **완료:** `END_OF_STREAM` 메시지 수신 시 스트리밍 종료

	""")
public interface ChatApi {
	@Operation(summary = "채팅방 목록 조회", description = "사용자의 채팅방 목록 정보(채팅방 Id, 제목)을 조회합니다.")
	BaseResponse<ChatsRes> getChats(Member member);

	@Operation(summary = "새로운 채팅 생성", description = "새로운 채팅을 생성합니다.")
	BaseResponse<CreateChatRes> createChat(
		CreateChatReq req,
		Member member
	);

	@Operation(summary = "링크 삭제", description = "해당 링크방과 채팅 기록을 전부 Hard Delete 진행합니다.")
	BaseResponse<String> deleteChat(Member member, Long chatId);

	void sendMessage(@Parameter(description = "채팅방 Id", required = true) Long chatId,
		@Parameter(description = "사용자 질문 내용", required = true) String message, Member member);

	void cancelMessage(@Parameter(description = "채팅방 Id", required = true) Long chatId, Member member);
}
