package com.sofa.linkiving.domain.chat.controller;

import com.sofa.linkiving.domain.member.entity.Member;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Chat", description = "AI 채팅 명세 (HTTP)")
public interface ChatApi {

	void sendMessage(@Parameter(description = "사용자 질문 내용", required = true) String message, Member member);

	void cancelMessage(Member member);
}
