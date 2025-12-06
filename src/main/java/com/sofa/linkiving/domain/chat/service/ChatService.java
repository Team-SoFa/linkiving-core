package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {
	private final ChatCommandService chatCommandService;
	private final ChatQueryService chatQueryService;
}
