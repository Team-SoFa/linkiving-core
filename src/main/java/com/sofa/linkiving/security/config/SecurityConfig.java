package com.sofa.linkiving.security.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sofa.linkiving.security.auth.config.OAuth2Properties;
import com.sofa.linkiving.security.auth.handler.OAuth2FailureHandler;
import com.sofa.linkiving.security.auth.handler.OAuth2SuccessHandler;
import com.sofa.linkiving.security.auth.service.CustomOAuth2UserService;
import com.sofa.linkiving.security.jwt.JwtProperties;
import com.sofa.linkiving.security.jwt.entrypoint.CustomAuthenticationEntryPoint;
import com.sofa.linkiving.security.jwt.filter.JwtAuthenticationFilter;
import com.sofa.linkiving.security.jwt.filter.JwtExceptionFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, OAuth2Properties.class})
public class SecurityConfig {

	private static final String[] PERMIT_URLS = {
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
		"/v1/member/**", "/mock/**", "/chat.html",

		/* oauth2 */
		"/oauth2/**"
	};
	private static final String[] SEMI_PERMIT_URLS = {
		//GET만 허용해야 하는 URL
	};

	private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtExceptionFilter jwtExceptionFilter;

	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	private final OAuth2FailureHandler oAuth2FailureHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.sessionManagement(sessionManagement ->
				sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize ->
				authorize
					.requestMatchers(PERMIT_URLS).permitAll()
					.anyRequest().authenticated()
			)
			.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo
					.userService(customOAuth2UserService)
				)
				.successHandler(oAuth2SuccessHandler)
				.failureHandler(oAuth2FailureHandler)
			)
			.exceptionHandling(exceptionConfig ->
				exceptionConfig.authenticationEntryPoint(customAuthenticationEntryPoint))
			.headers(headers ->
				headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class);

		return http.build();
	}
}
