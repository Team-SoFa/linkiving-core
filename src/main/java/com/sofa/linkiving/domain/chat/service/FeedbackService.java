package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {
	private final FeedbackQueryService feedbackQueryService;
	private final FeedbackCommandService feedbackCommandService;
}
