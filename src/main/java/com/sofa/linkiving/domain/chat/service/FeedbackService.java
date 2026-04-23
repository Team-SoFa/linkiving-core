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

	public Feedback upsertFeedback(Message message, Sentiment sentiment, String text) {
		return feedbackQueryService.findOptionalByMessage(message)
			.map(feedback -> {
				feedback.update(text, sentiment);
				return feedback;
			}).orElseGet(() -> {
				Feedback feedback = Feedback.builder()
					.message(message)
					.sentiment(sentiment)
					.text(text)
					.build();
				return feedbackCommandService.save(feedback);
			});
	}

	public void deleteAll(Chat chat) {
		feedbackCommandService.deleteFeedbacksByChat(chat);
	}
}
