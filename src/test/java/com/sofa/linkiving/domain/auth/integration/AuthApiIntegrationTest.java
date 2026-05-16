package com.sofa.linkiving.domain.auth.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.Cookie;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private RedisService redisService;

	private Member testMember;

	@BeforeEach
	void setUp() {
		testMember = memberRepository.save(Member.builder()
			.email("auth@test.com")
			.password("password")
			.build());
	}

	@Test
	@DisplayName("유효한 RefreshToken을 쿠키에 담아 재발급 요청 시 새로운 토큰 쿠키와 200 OK를 반환한다")
	void shouldReissueTokensAndReturnOk() throws Exception {
		// given
		String email = testMember.getEmail();
		String validRefreshToken = jwtTokenProvider.createRefreshToken(email);
		Cookie refreshTokenCookie = new Cookie("refreshToken", validRefreshToken);

		given(redisService.hasNoKey(any(), eq(email))).willReturn(false);
		given(redisService.get(any(), eq(email))).willReturn(validRefreshToken);

		// when & then
		mockMvc.perform(
				post("/v1/auth/reissue")
					.cookie(refreshTokenCookie)
					.with(csrf())
					.accept(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("토큰 재발급 완료"))
			.andExpect(cookie().exists("accessToken"))
			.andExpect(cookie().exists("refreshToken"));
	}

	@Test
	@DisplayName("RefreshToken 쿠키 없이 재발급 요청 시 에러를 반환한다")
	void shouldFailWhenRefreshTokenCookieIsMissing() throws Exception {
		// given - Cookie 설정 없음

		// when & then
		mockMvc.perform(
				post("/v1/auth/reissue")
					.with(csrf())
					.accept(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.data").value("J-003"));
	}
}
