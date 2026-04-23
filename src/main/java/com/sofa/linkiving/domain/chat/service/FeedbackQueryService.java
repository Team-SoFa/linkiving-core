package com.sofa.linkiving.domain.chat.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackQueryService {
	private final FeedbackRepository feedbackRepository;

	public Optional<Feedback> findOptionalByMessage(Message message) {
		return feedbackRepository.findByMessage(message);
	}
}
