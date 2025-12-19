package com.sofa.linkiving.domain.chat.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.chat.dto.response.AddFeedbackRes;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.service.FeedbackService;
import com.sofa.linkiving.domain.chat.service.MessageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackFacade {
	private final FeedbackService feedbackService;
	private final MessageService messageService;

	public AddFeedbackRes createFeedback(Long messageId, Sentiment sentiment, String text) {
		Message message = messageService.get(messageId);
		Long id = feedbackService.create(message, sentiment, text);
		return new AddFeedbackRes(id);
	}
}
