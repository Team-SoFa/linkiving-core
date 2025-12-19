package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackCommandService {
	private final FeedbackRepository feedbackRepository;

	public Feedback save(Feedback feedback) {
		return feedbackRepository.save(feedback);
	}
}
