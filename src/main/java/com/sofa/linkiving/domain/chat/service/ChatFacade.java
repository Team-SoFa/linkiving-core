package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatFacade {
	private final ChatService chatService;
	private final MessageService messageService;
	private final FeedbackService feedbackService;
}
