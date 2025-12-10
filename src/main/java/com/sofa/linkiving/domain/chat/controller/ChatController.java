package com.sofa.linkiving.domain.chat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.chat.dto.request.CreateChatReq;
import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.chat.dto.response.CreateChatRes;
import com.sofa.linkiving.domain.chat.facade.ChatFacade;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.security.annotation.AuthMember;

import jakarta.validation.Valid;
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
	public BaseResponse<CreateChatRes> createChat(@RequestBody @Valid CreateChatReq req, @AuthMember Member member) {
		CreateChatRes res = chatFacade.createChat(req.firstChat(), member);
		return BaseResponse.success(res, "채팅방 생성 완료");
	}
}
