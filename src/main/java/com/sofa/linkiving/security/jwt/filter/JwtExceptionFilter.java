package com.sofa.linkiving.security.jwt.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sofa.linkiving.security.jwt.entrypoint.CustomAuthenticationEntryPoint;
import com.sofa.linkiving.security.jwt.error.CustomJwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {
	private final CustomAuthenticationEntryPoint authenticationEntryPoint;

	@Override
	public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		} catch (CustomJwtException e) {
			request.setAttribute("exception", e.getErrorCode().getCode());

			authenticationEntryPoint.commence(
				request,
				response,
				new org.springframework.security.authentication.InsufficientAuthenticationException(
					"JWT error: " + e.getErrorCode().getCode(), e
				)
			);
		}
	}

}
