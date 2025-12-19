package com.sofa.linkiving.domain.chat.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class MockTitleClient implements TitleClient {
	@Override
	public String generateTitle(String firstChat) {
		return String.format("임시 제목[%s]", firstChat);
	}
}
