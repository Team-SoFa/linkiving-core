package com.sofa.linkiving.security.jwt.filter;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.global.error.code.CommonErrorCode;
import com.sofa.linkiving.security.auth.config.SecurityConstants;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Override
	public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String token = jwtTokenProvider.resolveToken(request);

		if (token != null && jwtTokenProvider.validateAccessToken(token)) {
			Authentication authentication = jwtTokenProvider.getAuthentication(token);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			if (isPendingTermsMember(authentication) && !isPendingTermsAllowed(request)) {
				response.setStatus(CommonErrorCode.FORBIDDEN.getStatus().value());
				response.setContentType("application/json;charset=UTF-8");
				objectMapper.writeValue(response.getWriter(), BaseResponse.error(CommonErrorCode.FORBIDDEN));
				return;
			}
			if (isWithdrawingMember(authentication) && !isWithdrawalRetryAllowed(request)) {
				response.setStatus(CommonErrorCode.FORBIDDEN.getStatus().value());
				response.setContentType("application/json;charset=UTF-8");
				objectMapper.writeValue(response.getWriter(), BaseResponse.error(CommonErrorCode.FORBIDDEN));
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private boolean isPendingTermsMember(Authentication authentication) {
		return authentication.getPrincipal() instanceof CustomMemberDetail memberDetail
			&& memberDetail.member().needsTermsAgreement();
	}

	private boolean isPendingTermsAllowed(HttpServletRequest request) {
		String path = request.getRequestURI();
		return Arrays.stream(SecurityConstants.PENDING_TERMS_ALLOWED_URLS)
			.anyMatch(pattern -> pathMatcher.match(pattern, path));
	}

	private boolean isWithdrawingMember(Authentication authentication) {
		return authentication.getPrincipal() instanceof CustomMemberDetail memberDetail
			&& memberDetail.member().isWithdrawing();
	}

	private boolean isWithdrawalRetryAllowed(HttpServletRequest request) {
		return "DELETE".equals(request.getMethod()) && "/v1/member".equals(request.getRequestURI());
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String path = request.getRequestURI();
		// 상수에 정의된 패턴 중 하나라도 일치하면 필터를 건너뜀
		return Arrays.stream(SecurityConstants.PERMIT_URLS)
			.anyMatch(pattern -> pathMatcher.match(pattern, path));
	}
}
