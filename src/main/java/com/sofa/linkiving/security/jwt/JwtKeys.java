package com.sofa.linkiving.security.jwt;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JwtKeys {
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Headers {
		public static final String AUTHORIZATION = "Authorization";
		public static final String BEARER_PREFIX = "Bearer ";
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Claims {
		public static final String TOKEN_TYPE = "token_type";
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class TokenType {
		public static final String ACCESS = "access";
		public static final String REFRESH = "refresh";
	}
}
