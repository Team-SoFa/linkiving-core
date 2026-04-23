package com.sofa.linkiving.domain.chat.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.chat.dto.response.UpsertFeedbackRes;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.service.FeedbackService;
import com.sofa.linkiving.domain.chat.service.MessageService;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackFacade {
	private final FeedbackService feedbackService;
	private final MessageService messageService;

	public UpsertFeedbackRes upsertFeedback(Member member, Long messageId, Sentiment sentiment, String text) {
		Message message = messageService.get(messageId, member);
		Long id = feedbackService.upsertFeedback(message, sentiment, text).getId();
		return new UpsertFeedbackRes(id);
	}
}
