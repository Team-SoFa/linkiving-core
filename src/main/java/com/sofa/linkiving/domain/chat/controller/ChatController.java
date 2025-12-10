package com.sofa.linkiving.domain.chat.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.chat.service.ChatService;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.security.annotation.AuthMember;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController implements ChatApi {
	private final ChatService chatService;

	@MessageMapping("send")
	@Override
	public void sendMessage(String message, @AuthMember Member member) {
		chatService.generateAnswer(member, message);
	}

	@MessageMapping("/cancel")
	@Override
	public void cancelMessage(@AuthMember Member member) {
		chatService.cancelAnswer(member);
	}
}
