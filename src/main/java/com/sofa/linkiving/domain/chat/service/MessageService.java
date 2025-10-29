package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {
	private final MessageCommandService messageCommandService;
	private final MessageQueryService messageQueryService;
}
