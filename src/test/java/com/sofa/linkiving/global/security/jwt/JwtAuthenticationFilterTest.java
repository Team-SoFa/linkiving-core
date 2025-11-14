package com.sofa.linkiving.global.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sofa.linkiving.security.jwt.JwtTokenProvider;
import com.sofa.linkiving.security.jwt.filter.JwtAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

public @ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	JwtTokenProvider jwtTokenProvider;
	@InjectMocks
	JwtAuthenticationFilter jwtAuthenticationFilter;

	@Test
	@DisplayName("유효 액세스 토큰 존재 시 SecurityContext에 인증 정보 저장")
	void shouldSetAuthenticationWhenValidAccessToken() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer good.token");

		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		Authentication authentication = new UsernamePasswordAuthenticationToken(
			"userId",
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);

		given(jwtTokenProvider.resolveToken(request)).willReturn("good.token");
		given(jwtTokenProvider.validateAccessToken("good.token")).willReturn(true);
		given(jwtTokenProvider.getAuthentication("good.token")).willReturn(authentication);

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, chain);

		// then
		Authentication result = SecurityContextHolder.getContext().getAuthentication();
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("userId");

		// verify
		verify(jwtTokenProvider, times(1)).resolveToken(request);
		verify(jwtTokenProvider, times(1)).validateAccessToken("good.token");
		verify(jwtTokenProvider, times(1)).getAuthentication("good.token");
		verify(chain, times(1)).doFilter(request, response);

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("토큰 미존재 시 SecurityContext 미설정 후 필터 체인 그대로 통과")
	void shouldSkipAuthenticationWhenNoToken() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		given(jwtTokenProvider.resolveToken(request)).willReturn(null);

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, chain);

		// then
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

		verify(jwtTokenProvider, times(1)).resolveToken(request);
		verify(jwtTokenProvider, never()).validateAccessToken(any());
		verify(jwtTokenProvider, never()).getAuthentication(any());
		verify(chain, times(1)).doFilter(request, response);

		SecurityContextHolder.clearContext();
	}
}
