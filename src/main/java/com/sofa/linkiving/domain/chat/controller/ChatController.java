package com.sofa.linkiving.domain.chat.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.chat.dto.request.CreateChatReq;
import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.chat.dto.response.CreateChatRes;
import com.sofa.linkiving.domain.chat.dto.response.MessagesRes;
import com.sofa.linkiving.domain.chat.facade.ChatFacade;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.security.annotation.AuthMember;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/chats")
@RequiredArgsConstructor
public class ChatController implements ChatApi {
	private final ChatFacade chatFacade;

	@Override
	@GetMapping
	public BaseResponse<ChatsRes> getChats(@AuthMember Member member) {
		ChatsRes res = chatFacade.getChats(member);
		return BaseResponse.success(res, "채팅방 목록 조회를 성공했습니다.");
	}

	@Override
	@PostMapping
	public BaseResponse<CreateChatRes> createChat(@RequestBody CreateChatReq req, @AuthMember Member member) {
		CreateChatRes res = chatFacade.createChat(req.firstChat(), member);
		return BaseResponse.success(res, "채팅방 생성 완료");
	}

	@Override
	@DeleteMapping("/{chatId}")
	public BaseResponse<String> deleteChat(@AuthMember Member member, @PathVariable Long chatId) {
		chatFacade.deleteChat(member, chatId);
		return BaseResponse.noContent("성공적으로 삭제했습니다.");
	}

	@Override
	@MessageMapping("/send/{chatId}")
	public void sendMessage(@DestinationVariable Long chatId, @Payload String message, @AuthMember Member member) {
		chatFacade.generateAnswer(chatId, member, message);
	}

	@Override
	@MessageMapping("/cancel/{chatId}")
	public void cancelMessage(@DestinationVariable Long chatId, @AuthMember Member member) {
		chatFacade.cancelAnswer(chatId, member);
	}

	@Override
	@GetMapping("/{chatId}")
	public BaseResponse<MessagesRes> getMessages(
		@AuthMember Member member,
		@PathVariable Long chatId,
		@RequestParam(required = false) Long lastId,
		@RequestParam(defaultValue = "20") int size
	) {
		MessagesRes res = chatFacade.getMessages(member, chatId, lastId, size);
		return BaseResponse.success(res, "채팅 기록을 가져오는데 성공했습니다.");
	}
}
