package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {
	private final FeedbackQueryService feedbackQueryService;
	private final FeedbackCommandService feedbackCommandService;

	public Long create(Message message, Sentiment sentiment, String text) {
		Feedback feedback = Feedback.builder()
			.message(message)
			.sentiment(sentiment)
			.text(text)
			.build();
		return feedbackCommandService.save(feedback).getId();
	}

	public void deleteAll(Chat chat) {
		feedbackCommandService.deleteFeedbacksByChat(chat);
	}
}
