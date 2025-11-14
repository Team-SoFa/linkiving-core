package com.sofa.linkiving.global.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.infra.redis.RedisKeyRegistry;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.jwt.JwtKeys;
import com.sofa.linkiving.security.jwt.JwtProperties;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;
import com.sofa.linkiving.security.jwt.error.CustomJwtException;
import com.sofa.linkiving.security.jwt.error.JwtErrorCode;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class JwtTokenProviderTest {

	private JwtTokenProvider createProvider(RedisService redisService, UserDetailsService uds) {
		String rawKey = "0123456789ABCDEF0123456789ABCDEF";
		String secret = Base64.getEncoder().encodeToString(rawKey.getBytes(StandardCharsets.UTF_8));

		JwtProperties jwtProperties = new JwtProperties(secret, 10L, 1L);

		if (uds == null) {
			uds = username -> new User(
				username, "", List.of(new SimpleGrantedAuthority("ROLE_" + Role.USER.name()))
			);
		}
		if (redisService == null) {
			redisService = mock(RedisService.class);
		}

		JwtTokenProvider provider = new JwtTokenProvider(jwtProperties, uds, redisService);
		provider.init();
		return provider;
	}

	@Test
	@DisplayName("액세스 토큰 생성 시 Authentication 복원 및 검증 성공")
	void shouldCreateAccessTokenAndRestoreAuthentication() {
		// given
		RedisService redisService = mock(RedisService.class);
		UserDetailsService uds = username -> new User(username, "",
			List.of(new SimpleGrantedAuthority("ROLE_" + Role.USER.name()))
		);

		JwtTokenProvider provider = createProvider(redisService, uds);
		String userId = "member";

		// when
		String token = provider.createAccessToken(userId);
		Authentication authentication = provider.getAuthentication(token);

		// then
		assertThat(token).isNotBlank();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo(userId);
		assertThat(authentication.getAuthorities())
			.extracting(GrantedAuthority::getAuthority)
			.contains("ROLE_USER");

		boolean valid = provider.validateAccessToken(token);
		assertThat(valid).isTrue();
	}

	@Test
	@DisplayName("리프레시 토큰 생성 시 Redis REFRESH_TOKEN 키로 저장")
	void shouldSaveRefreshTokenToRedis() {
		// given
		RedisService redisService = mock(RedisService.class);
		JwtTokenProvider provider = createProvider(redisService, null);
		String userId = "member";

		ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

		// when
		String refreshToken = provider.createRefreshToken(userId);

		// then
		assertThat(refreshToken).isNotBlank();
		verify(redisService, times(1))
			.save(eq(RedisKeyRegistry.REFRESH_TOKEN), tokenCaptor.capture(), eq(userId));

		assertThat(tokenCaptor.getValue()).isEqualTo(refreshToken);
	}

	@Test
	@DisplayName("빈 토큰 검증 시 EMPTY_TOKEN CustomJwtException 발생")
	void shouldThrowWhenAccessTokenIsBlank() {
		// given
		JwtTokenProvider provider = createProvider(mock(RedisService.class), null);

		// when & then
		assertThatThrownBy(() -> provider.validateAccessToken(" "))
			.isInstanceOf(CustomJwtException.class)
			.extracting("errorCode")
			.isEqualTo(JwtErrorCode.EMPTY_TOKEN);
	}

	@Test
	@DisplayName("형식 잘못된 토큰 검증 시 INVALID_JWT_TOKEN CustomJwtException 발생")
	void shouldThrowWhenAccessTokenIsMalformed() {
		// given
		JwtTokenProvider provider = createProvider(mock(RedisService.class), null);
		String badToken = "not-a-jwt-token";

		// when & then
		assertThatThrownBy(() -> provider.validateAccessToken(badToken))
			.isInstanceOf(CustomJwtException.class)
			.extracting("errorCode")
			.isEqualTo(JwtErrorCode.INVALID_JWT_TOKEN);
	}

	@Test
	@DisplayName("만료된 액세스 토큰 검증 시 EXPIRED_JWT_TOKEN CustomJwtException 발생")
	void shouldThrowWhenAccessTokenExpired() {
		// given
		RedisService redisService = mock(RedisService.class);
		JwtTokenProvider provider = createProvider(redisService, null);

		String rawKey = "0123456789ABCDEF0123456789ABCDEF";
		byte[] keyBytes = Decoders.BASE64.decode(
			Base64.getEncoder().encodeToString(rawKey.getBytes(StandardCharsets.UTF_8))
		);
		SecretKey key = Keys.hmacShaKeyFor(keyBytes);

		Date now = new Date();
		Date past = new Date(now.getTime() - 1000L);

		String expiredToken = io.jsonwebtoken.Jwts.builder()
			.subject("member")
			.claim(JwtKeys.Claims.TOKEN_TYPE, JwtKeys.TokenType.ACCESS)
			.issuedAt(past)
			.expiration(past)
			.signWith(key, io.jsonwebtoken.Jwts.SIG.HS256)
			.compact();

		// when & then
		assertThatThrownBy(() -> provider.validateAccessToken(expiredToken))
			.isInstanceOf(CustomJwtException.class)
			.extracting("errorCode")
			.isEqualTo(JwtErrorCode.EXPIRED_JWT_TOKEN);
	}

	@Test
	@DisplayName("리프레시 토큰 검증 시 Redis 미존재 시 CANNOT_REFRESH CustomJwtException 발생")
	void shouldThrowCannotRefreshWhenNoTokenInRedis() {
		// given
		RedisService redisService = mock(RedisService.class);
		JwtTokenProvider provider = createProvider(redisService, null);

		String userId = "member";
		String refreshToken = provider.createRefreshToken(userId);

		given(redisService.hasNoKey(RedisKeyRegistry.REFRESH_TOKEN, userId))
			.willReturn(true);

		// when & then
		assertThatThrownBy(() -> provider.validateRefreshToken(refreshToken, userId))
			.isInstanceOf(CustomJwtException.class)
			.extracting("errorCode")
			.isEqualTo(JwtErrorCode.CANNOT_REFRESH);
	}

	@Test
	@DisplayName("access 토큰을 refresh 검증에 사용 시 INVALID_REFRESH CustomJwtException 발생")
	void shouldThrowWhenRefreshTokenTypeIsInvalid() {
		// given
		RedisService redisService = mock(RedisService.class);
		JwtTokenProvider provider = createProvider(redisService, null);

		String accessToken = provider.createAccessToken("member");

		// when & then
		assertThatThrownBy(() -> provider.validateRefreshToken(accessToken, "member-5"))
			.isInstanceOf(CustomJwtException.class)
			.extracting("errorCode")
			.isEqualTo(JwtErrorCode.INVALID_REFRESH);
	}

	@Test
	@DisplayName("리프레시 토큰 검증 시 Redis 저장 토큰과 불일치 시 INVALID_JWT_TOKEN CustomJwtException 발생")
	void shouldThrowWhenRefreshTokenMismatch() {
		// given
		RedisService redisService = mock(RedisService.class);
		JwtTokenProvider provider = createProvider(redisService, null);

		String userId = "member";
		String refreshToken = provider.createRefreshToken(userId);

		given(redisService.hasNoKey(RedisKeyRegistry.REFRESH_TOKEN, userId)).willReturn(false);
		given(redisService.get(RedisKeyRegistry.REFRESH_TOKEN, userId))
			.willReturn("another-token");

		// when & then
		assertThatThrownBy(() -> provider.validateRefreshToken(refreshToken, userId))
			.isInstanceOf(CustomJwtException.class)
			.extracting("errorCode")
			.isEqualTo(JwtErrorCode.INVALID_JWT_TOKEN);
	}

	@Test
	@DisplayName("정상 리프레시 토큰 및 Redis 저장 토큰 일치 시 검증 통과")
	void shouldValidateRefreshTokenSuccessfully() {
		// given
		RedisService redisService = mock(RedisService.class);
		JwtTokenProvider provider = createProvider(redisService, null);

		String userId = "member";
		String refreshToken = provider.createRefreshToken(userId);

		given(redisService.hasNoKey(RedisKeyRegistry.REFRESH_TOKEN, userId)).willReturn(false);
		given(redisService.get(RedisKeyRegistry.REFRESH_TOKEN, userId)).willReturn(refreshToken);

		// when & then
		assertThatCode(() -> provider.validateRefreshToken(refreshToken, userId))
			.doesNotThrowAnyException();
	}
}
