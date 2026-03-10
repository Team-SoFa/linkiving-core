package com.sofa.linkiving.security.auth.config;

public abstract class SecurityConstants {
	public static final String[] PERMIT_URLS = {
		/* swagger */
		"/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources", "/swagger-resources/**",

		/* health check */
		"/health-check",

		/* favicon */
		"/favicon.ico",

		/* h2 */
		"/h2-console/**",

		/* web socket */
		"/ws/chat/**",

		/* temp */
		"/v1/member/signup", "/v1/member/login", "/mock/**",

		/* oauth2 */
		"/oauth2/**"
	};

	private static final String[] SEMI_PERMIT_URLS = {
		//GET만 허용해야 하는 URL
	};
}
