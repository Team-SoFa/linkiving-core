package com.sofa.linkiving.global.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import com.sofa.linkiving.security.jwt.entrypoint.CustomAuthenticationEntryPoint;
import com.sofa.linkiving.security.jwt.error.CustomJwtException;
import com.sofa.linkiving.security.jwt.error.JwtErrorCode;
import com.sofa.linkiving.security.jwt.filter.JwtExceptionFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
public class JwtExceptionFilterTest {

	@Mock
	CustomAuthenticationEntryPoint authenticationEntryPoint;

	@InjectMocks
	JwtExceptionFilter jwtExceptionFilter;

	@Test
	@DisplayName("하위 필터에서 CustomJwtException 발생 시 entryPoint 위임 및 예외 코드 request 저장")
	void shouldHandleCustomJwtExceptionAndDelegateToEntryPoint() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain chain = (req, res) -> {
			throw new CustomJwtException(JwtErrorCode.INVALID_JWT_TOKEN);
		};

		ArgumentCaptor<InsufficientAuthenticationException> exCaptor =
			ArgumentCaptor.forClass(InsufficientAuthenticationException.class);

		// when
		jwtExceptionFilter.doFilterInternal(request, response, chain);

		// then
		assertThat(request.getAttribute("exception"))
			.isEqualTo(JwtErrorCode.INVALID_JWT_TOKEN.getCode());

		verify(authenticationEntryPoint, times(1))
			.commence(eq(request), eq(response), exCaptor.capture());

		InsufficientAuthenticationException captured = exCaptor.getValue();
		assertThat(captured.getMessage()).contains(JwtErrorCode.INVALID_JWT_TOKEN.getCode());
	}

	@Test
	@DisplayName("예외 미발생 시 entryPoint 미호출 및 필터 체인 정상 통과")
	void shouldPassThroughWhenNoException() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain chain = mock(FilterChain.class);

		// when
		jwtExceptionFilter.doFilterInternal(request, response, chain);

		// then
		verify(chain, times(1)).doFilter(request, response);
		verifyNoInteractions(authenticationEntryPoint);
	}
}
