package com.sofa.linkiving.domain.chat.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackQueryService {
	private final FeedbackRepository feedbackRepository;
}
