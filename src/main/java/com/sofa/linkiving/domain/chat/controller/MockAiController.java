package com.sofa.linkiving.domain.chat.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/mock/ai")
public class MockAiController {

	@PostMapping(value = "/generate", produces = MediaType.APPLICATION_NDJSON_VALUE) // 또는 TEXT_EVENT_STREAM_VALUE
	public Flux<String> generateAnswer(@RequestBody Map<String, String> request) {
		String userPrompt = request.get("prompt");

		String fakeResponse = """
			안녕하세요! 저는 임시 AI 봇입니다. 🤖
			현재 AI 서버가 구축되지 않아서 테스트용 답변을 드리고 있어요.
			질문하신 내용인 "%s"에 대해 답변을 생성하는 척 하고 있습니다.
			취소 기능을 테스트하시려면 지금 바로 취소 버튼을 눌러보세요!
			타이핑 효과를 위해 천천히 답변을 보내고 있습니다...
			""".formatted(userPrompt);

		return Flux.fromArray(fakeResponse.split(""))
			.delayElements(Duration.ofMillis(100));
	}
}
