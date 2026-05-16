package com.sofa.linkiving.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.auth.dto.internal.TokenDto;
import com.sofa.linkiving.security.jwt.JwtProperties;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;
import com.sofa.linkiving.security.jwt.error.CustomJwtException;
import com.sofa.linkiving.security.jwt.error.JwtErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
public class AuthServiceTest {

	@InjectMocks
	private AuthService authService;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private JwtProperties jwtProperties;

	@Test
	@DisplayName("유효한 RefreshToken이 주어지면 새로운 토큰 쌍을 발급한다")
	void shouldReissueTokensSuccessfully() {
		// given

		String oldRefreshToken = "old-refresh-token";

		String newAccessToken = "new-access-token";
		String newRefreshToken = "new-refresh-token";

		given(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).willReturn("test@test.com");
		given(jwtTokenProvider.createAccessToken("test@test.com")).willReturn(newAccessToken);
		given(jwtTokenProvider.createRefreshToken("test@test.com")).willReturn(newRefreshToken);

		// 1시간 = 3600000ms, 2주 = 1209600000ms
		given(jwtProperties.accessTokenValidTime()).willReturn(3600000L);
		given(jwtProperties.refreshTokenValidTime()).willReturn(1209600000L);

		// when
		TokenDto result = authService.reissue(oldRefreshToken);

		// then
		assertThat(result).isNotNull();
		assertThat(result.accessToken()).isEqualTo(newAccessToken);
		assertThat(result.accessExp()).isEqualTo(3600);
		assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
		assertThat(result.refreshExp()).isEqualTo(1209600);

		verify(jwtTokenProvider, times(1)).validateRefreshToken(oldRefreshToken);
	}

	@Test
	@DisplayName("RefreshToken 검증에 실패하면 예외가 발생한다")
	void shouldThrowExceptionWhenRefreshTokenIsInvalid() {
		// given
		String invalidToken = "invalid-token";

		willThrow(new CustomJwtException(JwtErrorCode.INVALID_JWT_TOKEN))
			.given(jwtTokenProvider).validateRefreshToken(invalidToken);

		// when & then
		assertThatThrownBy(() -> authService.reissue(invalidToken))
			.isInstanceOf(RuntimeException.class);
	}
}
