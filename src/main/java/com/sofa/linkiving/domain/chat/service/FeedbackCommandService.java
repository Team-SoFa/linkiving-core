package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackCommandService {
	private final FeedbackRepository feedbackRepository;

	public void deleteFeedbacksByChat(Chat chat) {
		feedbackRepository.deleteAllByChat(chat);
	}
}
