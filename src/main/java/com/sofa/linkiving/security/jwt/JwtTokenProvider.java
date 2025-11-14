package com.sofa.linkiving.security.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.infra.redis.RedisKeyRegistry;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.jwt.error.CustomJwtException;
import com.sofa.linkiving.security.jwt.error.JwtErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
	private final JwtProperties jwtProperties;
	private final UserDetailsService userDetailsService;
	private final RedisService redisService;

	private SecretKey secretKey;
	private long accessTokenValidTime;
	private long refreshTokenValidTime;

	@PostConstruct
	public void init() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
		if (keyBytes.length < 32) {
			throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes) for HS256");
		}
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);

		this.accessTokenValidTime = jwtProperties.accessTokenValidTime();
		this.refreshTokenValidTime = jwtProperties.refreshTokenValidTime();
	}

	private String createJwtToken(String subject, long validityMillis, String tokenType) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + validityMillis);

		return Jwts.builder()
			.subject(subject)
			.claim(JwtKeys.Claims.TOKEN_TYPE, tokenType)
			.issuedAt(now)
			.expiration(exp)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	public String createAccessToken(String id) {
		return createJwtToken(id, accessTokenValidTime, JwtKeys.TokenType.ACCESS);
	}

	public String createRefreshToken(String id) {
		String token = createJwtToken(id, refreshTokenValidTime, JwtKeys.TokenType.REFRESH);
		redisService.save(RedisKeyRegistry.REFRESH_TOKEN, token, id);

		return token;

	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public Authentication getAuthentication(String token) {
		String userId = parseClaims(token).getSubject();
		UserDetails principal = userDetailsService.loadUserByUsername(userId);
		return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
	}

	public String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader(JwtKeys.Headers.AUTHORIZATION);

		if (bearer == null) {
			bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
		}

		if (bearer != null && bearer.startsWith(JwtKeys.Headers.BEARER_PREFIX)) {
			return bearer.substring(JwtKeys.Headers.BEARER_PREFIX.length());
		}
		return null;
	}

	public Date getExpiration(String token) {
		return parseClaims(token).getExpiration();
	}

	public String getUserIdFromToken(String token) {
		return parseClaims(token).getSubject();
	}

	public void validateRefreshToken(String refreshToken, String userId) {
		Claims claims = parseClaims(refreshToken);
		String tokenType = claims.get(JwtKeys.Claims.TOKEN_TYPE, String.class);

		if (!JwtKeys.TokenType.REFRESH.equals(tokenType)) {
			throw new CustomJwtException(JwtErrorCode.INVALID_REFRESH);
		}

		if (redisService.hasNoKey(RedisKeyRegistry.REFRESH_TOKEN, userId)) {
			throw new CustomJwtException(JwtErrorCode.CANNOT_REFRESH);
		}

		String redisToken = redisService.get(RedisKeyRegistry.REFRESH_TOKEN, userId);

		if (!redisToken.equals(refreshToken)) {
			throw new CustomJwtException(JwtErrorCode.INVALID_JWT_TOKEN);
		}

	}

	public boolean validateAccessToken(String token) {
		if (token == null || token.isBlank()) {
			throw new CustomJwtException(JwtErrorCode.EMPTY_TOKEN);
		}

		try {
			Claims claims = parseClaims(token);
			boolean notExpired = !claims.getExpiration().before(new Date());

			String tokenType = claims.get(JwtKeys.Claims.TOKEN_TYPE, String.class);
			boolean isAccess = JwtKeys.TokenType.ACCESS.equals(tokenType);

			return notExpired && isAccess;
		} catch (SecurityException | MalformedJwtException | IllegalArgumentException e) {
			throw new CustomJwtException(JwtErrorCode.INVALID_JWT_TOKEN);
		} catch (ExpiredJwtException e) {
			throw new CustomJwtException(JwtErrorCode.EXPIRED_JWT_TOKEN);
		} catch (UnsupportedJwtException e) {
			throw new CustomJwtException(JwtErrorCode.UNSUPPORTED_JWT_TOKEN);
		}
	}
}
