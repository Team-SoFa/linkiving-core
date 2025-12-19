package com.sofa.linkiving.domain.chat.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.chat.dto.request.TitleGenerateReq;
import com.sofa.linkiving.domain.chat.dto.response.TitleGenerateRes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class RagTitleClient implements TitleClient {

	private static final int MAX_TITLE_LENGTH = 100;
	private final RagTitleFeign ragTitleFeign;

	@Override
	public String generateTitle(String firstChat) {
		try {
			List<TitleGenerateRes> response = ragTitleFeign.generateTitle(new TitleGenerateReq(firstChat));

			if (response == null || response.isEmpty()) {
				return truncateTitle(firstChat);
			}

			return response.get(0).title();

		} catch (Exception e) {
			log.error("AI 서버 통신 실패. 기본 제목으로 대체합니다. error={}", e.getMessage());
			return truncateTitle(firstChat);
		}
	}

	private String truncateTitle(String originalTitle) {
		if (originalTitle.length() <= MAX_TITLE_LENGTH) {
			return originalTitle;
		}
		return originalTitle.substring(0, MAX_TITLE_LENGTH);
	}
}
