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
public class RealAiTitleClient implements AiTitleClient {

	private final AiTitleFeign aiTitleFeign;

	@Override
	public String generateSummary(String firstChat) {
		try {
			List<TitleGenerateRes> response = aiTitleFeign.generateTitle(new TitleGenerateReq(firstChat));

			if (response == null || response.isEmpty()) {
				return firstChat;
			}

			return response.get(0).title();

		} catch (Exception e) {
			log.error("AI 서버 통신 실패. 기본 제목으로 대체합니다. error={}", e.getMessage());
			return firstChat;
		}
	}
}
