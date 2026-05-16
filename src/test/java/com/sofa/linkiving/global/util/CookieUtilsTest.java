package com.sofa.linkiving.global.util;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sofa.linkiving.global.config.CookieProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("CookieUtils 단위 테스트")
class CookieUtilsTest {

	@InjectMocks
	private CookieUtils cookieUtils;

	@Mock
	private CookieProperties cookieProperties;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@Test
	@DisplayName("로컬 환경(localhost)에서는 httpOnly=false, secure=false, SameSite=Lax 로 쿠키가 생성된다")
	void shouldAddCookieInLocalEnvironment() {
		// given
		request.setServerName("localhost");

		// when
		cookieUtils.addCookie(request, response, "accessToken", "token-value", 3600);

		// then
		String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookieHeader).isNotNull();
		assertThat(setCookieHeader).contains("accessToken=token-value");
		assertThat(setCookieHeader).contains("Max-Age=3600");
		assertThat(setCookieHeader).contains("Path=/");
		assertThat(setCookieHeader).contains("SameSite=Lax");
		assertThat(setCookieHeader).doesNotContain("HttpOnly");
		assertThat(setCookieHeader).doesNotContain("Secure");
	}

	@Test
	@DisplayName("운영 환경(localhost가 아님)에서는 httpOnly=true, secure=true, SameSite=None 으로 쿠키가 생성된다")
	void shouldAddCookieInProdEnvironment() {
		// given
		request.setServerName("api.linkiving.com");
		given(cookieProperties.domain()).willReturn("linkiving.com");

		// when
		cookieUtils.addCookie(request, response, "accessToken", "token-value", 3600);

		// then
		String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookieHeader).isNotNull();
		assertThat(setCookieHeader).contains("accessToken=token-value");
		assertThat(setCookieHeader).contains("Max-Age=3600");
		assertThat(setCookieHeader).contains("Path=/");
		assertThat(setCookieHeader).contains("SameSite=None");
		assertThat(setCookieHeader).contains("HttpOnly");
		assertThat(setCookieHeader).contains("Secure");
		assertThat(setCookieHeader).contains("Domain=linkiving.com");
	}
}
