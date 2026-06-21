package com.sofa.linkiving.global.logging;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class RequestContextInterceptorTest {

	private final RequestContextInterceptor interceptor = new RequestContextInterceptor();

	@AfterEach
	void tearDown() {
		MDC.clear();
	}

	@Test
	@DisplayName("링크 API의 path variable id는 linkId MDC로 기록한다")
	void shouldPopulateLinkIdFromLinkRouteId() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/links/123");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("id", "123"));

		boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

		assertThat(result).isTrue();
		assertThat(MDC.get(LogContext.LINK_ID)).isEqualTo("123");
	}
}
