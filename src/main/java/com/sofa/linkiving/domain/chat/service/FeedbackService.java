package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {
	private final FeedbackQueryService feedbackQueryService;
	private final FeedbackCommandService feedbackCommandService;

	public void deleteAll(Chat chat) {
		feedbackCommandService.deleteFeedbacksByChat(chat);
	}
}
