package com.sofa.linkiving.domain.member.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.domain.member.dto.request.LoginReq;
import com.sofa.linkiving.domain.member.dto.request.SignupReq;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.infra.redis.RedisKeyRegistry;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class MemberApiIntegrationTest {

	private static final String BASE_URL = "/v1/member";
	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	MemberRepository memberRepository;
	@MockitoBean
	RedisService redisService;

	@Test
	@DisplayName("회원가입 성공 시 DB에 인코딩된 비밀번호가 저장된다")
	void shouldSignupSuccessfullyWithRealRepository() throws Exception {
		// given
		String url = BASE_URL + "/signup";

		String email = "test@test.com";
		String password = "test";
		SignupReq req = new SignupReq(email, password);

		String expectedEncoded = Base64.getEncoder()
			.encodeToString(password.getBytes(StandardCharsets.UTF_8));

		// when & then
		mockMvc.perform(
				post(url)
					.with(csrf())
					.with(user("tester").roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.status").value("OK"))
			.andExpect(jsonPath("$.message").value("회원 가입에 성공하였습니다."))
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

		Member saved = memberRepository.findByEmail(email).orElseThrow();
		assertThat(saved.getEmail()).isEqualTo(email);
		assertThat(saved.getPassword()).isEqualTo(expectedEncoded);
	}

	@Test
	@DisplayName("회원가입 실패 시 중복 이메일로 인한 예외가 발생한다")
	void shouldFailWhenEmailAlreadyExistsWithRealRepository() throws Exception {
		// given: 이미 저장된 사용자
		String url = BASE_URL + "/signup";

		String email = "test@test.com";
		String password = "test";
		String encoded = Base64.getEncoder()
			.encodeToString(password.getBytes(StandardCharsets.UTF_8));

		memberRepository.save(Member.builder()
			.email(email)
			.password(encoded)
			.build());

		SignupReq req = new SignupReq(email, "newPassword");

		// when & then
		mockMvc.perform(
				post(url)
					.with(csrf())
					.with(user("tester").roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
			)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.status").value(MemberErrorCode.DUPLICATE_EMAIL.getStatus().name()))
			.andExpect(jsonPath("$.message").value(MemberErrorCode.DUPLICATE_EMAIL.getMessage()))
			.andExpect(jsonPath("$.data").value(MemberErrorCode.DUPLICATE_EMAIL.getCode()));
	}

	@Test
	@DisplayName("올바른 이메일과 비밀번호로 로그인 시 성공")
	void shouldLoginSuccess() throws Exception {
		// given
		String url = BASE_URL + "/login";

		String email = "test@test.com";
		String rawPassword = "test";
		String encoded = Base64.getEncoder()
			.encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));

		memberRepository.save(Member.builder()
			.email(email)
			.password(encoded)
			.build());

		LoginReq req = new LoginReq(email, rawPassword);

		// when & then
		mockMvc.perform(post(url)
				.with(csrf())
				.with(user("tester").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.status").value("OK"))
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
			.andExpect(jsonPath("$.message").value("로그인에 성공하였습니다."));

		Member saved = memberRepository.findByEmail(email).orElseThrow();
		assertThat(saved.getEmail()).isEqualTo(email);
		assertThat(saved.getPassword()).isEqualTo(encoded);
	}

	@Test
	@DisplayName("잘못된 비밀번호로 로그인 시 400과 INCORRECT_PASSWORD 코드 반환")
	void shouldFailWhenIncorrectPassword() throws Exception {
		// given
		String url = BASE_URL + "/login";

		String email = "test@test.com";
		String correctPassword = "test";
		String wrongPassword = "wrong";
		String encoded = Base64.getEncoder()
			.encodeToString(correctPassword.getBytes(StandardCharsets.UTF_8));

		memberRepository.save(Member.builder()
			.email(email)
			.password(encoded)
			.build());

		LoginReq req = new LoginReq(email, wrongPassword);

		// when & then
		mockMvc.perform(post(url)
				.with(csrf())
				.with(user("tester").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.status").value(MemberErrorCode.INCORRECT_PASSWORD.getStatus().name()))
			.andExpect(jsonPath("$.message").value(MemberErrorCode.INCORRECT_PASSWORD.getMessage()))
			.andExpect(jsonPath("$.data").value(MemberErrorCode.INCORRECT_PASSWORD.getCode()));
	}

	@Test
	@DisplayName("로그아웃 시 로컬 환경에서는 HttpOnly/Secure 없이 쿠키가 만료된다")
	void shouldExpireCookiesWithoutSecureFlagsOnLocalhost() throws Exception {
		// given
		Member member = memberRepository.save(Member.builder()
			.email("logout-local@test.com")
			.password("password")
			.build());
		CustomMemberDetail userDetails = new CustomMemberDetail(member, Role.USER);

		// when
		MvcResult result = mockMvc.perform(post(BASE_URL + "/logout")
					.with(csrf())
					.with(user(userDetails))
					.with(request -> {
						request.setServerName("localhost");
						return request;
					})
					.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("로그아웃에 성공하였습니다."))
			.andReturn();

		// then
		verify(redisService).delete(RedisKeyRegistry.REFRESH_TOKEN, member.getEmail());

		List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
		assertThat(setCookies).hasSize(2);

		String accessTokenCookie = setCookies.stream()
			.filter(cookie -> cookie.startsWith("accessToken="))
			.findFirst()
			.orElseThrow();
		String refreshTokenCookie = setCookies.stream()
			.filter(cookie -> cookie.startsWith("refreshToken="))
			.findFirst()
			.orElseThrow();

		assertThat(accessTokenCookie).contains("Max-Age=0", "Path=/", "SameSite=Lax");
		assertThat(refreshTokenCookie).contains("Max-Age=0", "Path=/", "SameSite=Lax");
		assertThat(accessTokenCookie).doesNotContain("HttpOnly");
		assertThat(accessTokenCookie).doesNotContain("Secure");
		assertThat(refreshTokenCookie).doesNotContain("HttpOnly");
		assertThat(refreshTokenCookie).doesNotContain("Secure");
	}

	@Test
	@DisplayName("로그아웃 시 운영 환경에서는 HttpOnly/Secure 쿠키로 만료된다")
	void shouldExpireCookiesWithSecureFlagsOnNonLocalhost() throws Exception {
		// given
		Member member = memberRepository.save(Member.builder()
			.email("logout-prod@test.com")
			.password("password")
			.build());
		CustomMemberDetail userDetails = new CustomMemberDetail(member, Role.USER);

		// when
		MvcResult result = mockMvc.perform(post(BASE_URL + "/logout")
					.with(csrf())
					.with(user(userDetails))
					.with(request -> {
						request.setServerName("example.com");
						return request;
					})
					.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("로그아웃에 성공하였습니다."))
			.andReturn();

		// then
		verify(redisService).delete(RedisKeyRegistry.REFRESH_TOKEN, member.getEmail());

		List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
		assertThat(setCookies).hasSize(2);

		String accessTokenCookie = setCookies.stream()
			.filter(cookie -> cookie.startsWith("accessToken="))
			.findFirst()
			.orElseThrow();
		String refreshTokenCookie = setCookies.stream()
			.filter(cookie -> cookie.startsWith("refreshToken="))
			.findFirst()
			.orElseThrow();

		assertThat(accessTokenCookie).contains("Max-Age=0", "Path=/", "SameSite=Lax");
		assertThat(refreshTokenCookie).contains("Max-Age=0", "Path=/", "SameSite=Lax");
		assertThat(accessTokenCookie).contains("HttpOnly");
		assertThat(accessTokenCookie).contains("Secure");
		assertThat(refreshTokenCookie).contains("HttpOnly");
		assertThat(refreshTokenCookie).contains("Secure");
	}
}
