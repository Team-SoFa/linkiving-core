package com.sofa.linkiving.domain.member.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.member.dto.request.TermsAgreementReq;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.domain.member.service.MemberService;
import com.sofa.linkiving.infra.redis.RedisKeyRegistry;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"app.cookie.domain=linkiving.com",
	"ai.server.url=http://localhost",
	"test.external.base-url=http://localhost",
	"security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
	"security.jwt.access-token-valid-minute=10",
	"security.jwt.refresh-token-valid-month=1",
	"spring.cloud.aws.s3.bucket=test-bucket",
	"spring.cloud.aws.region.static=ap-northeast-2",
	"summary.worker.sleep-duration=10ms",
	"hashids.salt=test-salt",
	"spring.data.redis.host=localhost",
	"spring.data.redis.port=6379"
})
public class MemberApiIntegrationTest {

	private static final String BASE_URL = "/v1/member";
	@Autowired
	MockMvc mockMvc;
	@Autowired
	MemberRepository memberRepository;
	@Autowired
	MemberService memberService;
	@Autowired
	EntityManager entityManager;
	@MockitoBean
	RedisService redisService;
	@MockitoBean
	ClientRegistrationRepository clientRegistrationRepository;

	@Test
	@DisplayName("로그아웃 시 로컬 환경에서는 HttpOnly/Secure 없이 쿠키가 만료된다")
	void shouldExpireCookiesWithoutSecureFlagsOnLocalhost() throws Exception {
		// given
		Member member = memberRepository.save(Member.builder()
			.email("logout-local@test.com")
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
			.build());
		CustomMemberDetail userDetails = new CustomMemberDetail(member, Role.USER);

		// when
		MvcResult result = mockMvc.perform(post(BASE_URL + "/logout")
				.with(csrf())
				.with(user(userDetails))
				.with(request -> {
					request.setServerName("api.linkiving.com");
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

		assertThat(accessTokenCookie).contains("Max-Age=0", "Path=/", "SameSite=None", "Domain=linkiving.com");
		assertThat(refreshTokenCookie).contains("Max-Age=0", "Path=/", "SameSite=None", "Domain=linkiving.com");
		assertThat(accessTokenCookie).contains("HttpOnly");
		assertThat(accessTokenCookie).contains("Secure");
		assertThat(refreshTokenCookie).contains("HttpOnly");
		assertThat(refreshTokenCookie).contains("Secure");
	}

	@Test
	void shouldPersistTermsAgreementWhenAuthMemberIsDetached() {
		// given
		Member saved = memberRepository.save(Member.builder()
			.email("terms-detached@test.com")
			.status(MemberStatus.PENDING_TERMS)
			.build());
		entityManager.flush();
		entityManager.detach(saved);

		TermsAgreementReq req = new TermsAgreementReq(true, true, "2026-08-03", "2026-08-03");

		// when
		memberService.agreeTerms(saved, req);
		entityManager.flush();
		entityManager.clear();

		// then
		Member reloaded = memberRepository.findByEmail(saved.getEmail()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		assertThat(reloaded.getTermsVersion()).isEqualTo("2026-08-03");
		assertThat(reloaded.getPrivacyVersion()).isEqualTo("2026-08-03");
		assertThat(reloaded.getTermsAgreedAt()).isNotNull();
		assertThat(reloaded.getPrivacyAgreedAt()).isNotNull();
	}
}
