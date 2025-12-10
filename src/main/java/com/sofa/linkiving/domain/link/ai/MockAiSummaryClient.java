package com.sofa.linkiving.domain.link.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.enums.Format;

@Component
@Primary
public class MockAiSummaryClient implements AiSummaryClient {

	@Override
	public String generateSummary(Long linkId, String url, Format format) {

		if (format == Format.DETAILED) {
			return """
				[자세한 요약 (Mock)]
				OpenFeign 도입을 대비하여 Interface 기반 설계를 적용했습니다.
				1. AiSummaryClient 인터페이스를 정의하여 의존성을 역전시켰습니다.
				2. 현재는 MockAiSummaryClient가 동작하지만, 추후 실제 구현체로 교체하기 쉽습니다.
				3. 비즈니스 로직은 AI 서버의 통신 방식(HTTP, gRPC 등)에 영향을 받지 않습니다.
				""";
		} else {
			return """
				[간결한 요약 (Mock)]
				OpenFeign 도입을 위해 인터페이스 패턴을 적용하여, 코드 수정 없이 구현체 교체가 가능한 확장성 있는 구조를 만들었습니다.
				""";
		}
	}

	@Override
	public String comparisonSummary(String existingSummary, String newSummary) {
		return """
			[변경 사항 분석]
			기존 요약 대비 다음 내용이 보강되었습니다:
			- AI 아키텍처 설계 방식에 대한 구체적인 설명 추가
			- OpenFeign 도입의 이점 명시
			(이 내용은 Mock 데이터이며, 실제 AI는 두 텍스트의 차이를 분석하여 제공합니다.)
			""";
	}
}
