package com.sofa.linkiving.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import com.sofa.linkiving.global.config.CorsProperties;
import com.sofa.linkiving.security.auth.handler.OAuth2FailureHandler;
import com.sofa.linkiving.security.auth.handler.OAuth2SuccessHandler;
import com.sofa.linkiving.security.auth.service.CustomOAuth2UserService;
import com.sofa.linkiving.security.jwt.entrypoint.CustomAuthenticationEntryPoint;
import com.sofa.linkiving.security.jwt.filter.JwtAuthenticationFilter;
import com.sofa.linkiving.security.jwt.filter.JwtExceptionFilter;

@DisplayName("SecurityConfig CORS 단위 테스트")
class SecurityConfigTest {

	private static final String WEB_ORIGIN = "http://localhost:3000";
	private static final String EXTENSION_ORIGIN = "chrome-extension://dchepopfkpdcmpgpgbodmgblmnnagcdd";

	@Test
	@DisplayName("웹 origin과 익스텐션 origin을 모두 허용한다")
	void shouldAllowWebAndExtensionOrigins() {
		// given
		SecurityConfig securityConfig = createSecurityConfig(
			new CorsProperties(List.of(WEB_ORIGIN), List.of(EXTENSION_ORIGIN)));

		// when
		CorsConfiguration corsConfiguration = securityConfig.corsConfigurationSource()
			.getCorsConfiguration(new MockHttpServletRequest());

		// then
		assertThat(corsConfiguration).isNotNull();
		assertThat(corsConfiguration.getAllowedOrigins())
			.containsExactly(WEB_ORIGIN, EXTENSION_ORIGIN);
	}

	@Test
	@DisplayName("익스텐션 origin이 설정되지 않으면 웹 origin만 허용한다")
	void shouldAllowOnlyWebOriginsWhenExtensionOriginsAreMissing() {
		// given
		SecurityConfig securityConfig = createSecurityConfig(
			new CorsProperties(List.of(WEB_ORIGIN), null));

		// when
		CorsConfiguration corsConfiguration = securityConfig.corsConfigurationSource()
			.getCorsConfiguration(new MockHttpServletRequest());

		// then
		assertThat(corsConfiguration).isNotNull();
		assertThat(corsConfiguration.getAllowedOrigins()).containsExactly(WEB_ORIGIN);
	}

	@Test
	@DisplayName("중복된 origin은 한 번만 허용한다")
	void shouldDeduplicateAllowedOrigins() {
		// given
		SecurityConfig securityConfig = createSecurityConfig(
			new CorsProperties(List.of(WEB_ORIGIN, EXTENSION_ORIGIN), List.of(EXTENSION_ORIGIN)));

		// when
		CorsConfiguration corsConfiguration = securityConfig.corsConfigurationSource()
			.getCorsConfiguration(new MockHttpServletRequest());

		// then
		assertThat(corsConfiguration).isNotNull();
		assertThat(corsConfiguration.getAllowedOrigins())
			.containsExactly(WEB_ORIGIN, EXTENSION_ORIGIN);
	}

	private SecurityConfig createSecurityConfig(CorsProperties corsProperties) {
		return new SecurityConfig(
			mock(CustomAuthenticationEntryPoint.class),
			mock(JwtAuthenticationFilter.class),
			mock(JwtExceptionFilter.class),
			mock(CustomOAuth2UserService.class),
			mock(OAuth2SuccessHandler.class),
			mock(OAuth2FailureHandler.class),
			corsProperties
		);
	}
}
