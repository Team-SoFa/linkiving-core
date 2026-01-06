package com.sofa.linkiving.domain.chat.controller;

import org.springframework.validation.annotation.Validated;

import com.sofa.linkiving.domain.chat.dto.request.AnswerCancelReq;
import com.sofa.linkiving.domain.chat.dto.request.AnswerReq;
import com.sofa.linkiving.domain.chat.dto.request.CreateChatReq;
import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.chat.dto.response.CreateChatRes;
import com.sofa.linkiving.domain.chat.dto.response.MessagesRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@Tag(name = "Chat", description = """
	AI 채팅 통합 명세 (HTTP + WebSocket)

	### 📡 1. WebSocket 연결 정보
		* **Socket Endpoint:** `ws://{domain}/ws/chat`
		* **Subscribe Path:** `/user/queue/chat` (전역 구독)

	### 🚀 2. 동작 흐름
		1. **소켓 연결:** 로그인 직후 `/user/queue/chat` 구독
		2. **질문 전송:** `/send` 로 요청 전송
			- Body: `{ "chatId": 1, "message": "질문" }`
		3. **답변 수신:** 구독한 경로로 답변 도착 (chatId 포함됨)
			**CASE A: 답변 생성 성공 (success: true)**
				- AI의 답변과 참고 링크가 포함됩니다.
				```json
					{
					"success": true,
					"chatId": 1,
					"messageId": 105,
					"content": "질문하신 내용에 대한 AI 답변입니다...",
					"step": ["질문 분석", "데이터 검색", "답변 생성"],
					"links": [
						{ "linkId": 10, "title": "관련 문서 제목", "url": "https://...", "imageUrl": "http://...", "summary": "요약 내용" }
					]
				}
				```

			**CASE B: 답변 생성 실패 (success: false)**
				- 에러 상황입니다. `content` 필드에 **사용자가 보냈던 원래 질문**이 담겨옵니다.
				- 프론트엔드 처리: 이 값을 다시 입력창(Input)에 채워주세요.
					```json
					{
						"success": false,
						"chatId": 1,
						"messageId": null,
						"content": "내 질문 내용",
						"step": null,
						"links": null
					}
					```
		4. **답변 취소**: `/cancel` 로 요청 전송
			- Body: `{ "chatId": 1 }`
	""")
public interface ChatApi {
	@Operation(summary = "채팅 기록 조회", description = "채팅 기록을 최신순으로 조회합니다. 무한 스크롤 방식으로 제공됩니다.")
	BaseResponse<MessagesRes> getMessages(
		Member member,
		@Parameter(description = "채팅방 ID") Long chatId,
		@Parameter(description = "페이징을 위한 마지막 메시지 ID, 첫 조회 시 null") Long lastId,

		@Parameter(description = "페이지 크기")
		@Min(value = 1, message = "최소 1개 이상 조회해야 합니다.")
		@Max(value = 50, message = "한 번에 최대 50개까지만 조회할 수 있습니다.")
		int size
	);

	@Operation(summary = "채팅방 목록 조회", description = "사용자의 채팅방 목록 정보(채팅방 Id, 제목)을 조회합니다.")
	BaseResponse<ChatsRes> getChats(Member member);

	@Operation(summary = "새로운 채팅 생성", description = "새로운 채팅을 생성합니다.")
	BaseResponse<CreateChatRes> createChat(
		@Valid CreateChatReq req,
		Member member
	);

	@Operation(summary = "링크 삭제", description = "해당 링크방과 채팅 기록을 전부 Hard Delete 진행합니다.")
	BaseResponse<String> deleteChat(Member member, Long chatId);

	void sendMessage(AnswerReq req, Member member);

	void cancelMessage(AnswerCancelReq req, Member member);
}
