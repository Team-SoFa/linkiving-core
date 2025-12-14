package com.sofa.linkiving.domain.chat.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class MockAiTitleClient implements AiTitleClient {
	@Override
	public String generateSummary(String firstChat) {
		return String.format("임시 제목[%s]", firstChat);
	}
}
