package com.sofa.linkiving.security.resolver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.security.annotation.AuthMember;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthMemberArgumentResolverTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("인증된 CustomMemberDetail 존재 시 Member 객체 정상 주입")
	void shouldResolveMemberWhenAuthenticated() throws Exception {
		// given
		Member member = Member.builder()
			.email("test@test.com")
			.build();

		CustomMemberDetail userDetails = new CustomMemberDetail(member, member.getRole());

		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
		);

		// when & then
		mockMvc.perform(get("/test/auth-member")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().string("Resolved: test@test.com"))
			.andDo(print());
	}

	@Test
	@DisplayName("유효하지 않은 토큰 존재 시 401 Unauthorized 및 C-005 에러 코드 반환")
	@WithAnonymousUser
	void shouldThrowUnauthorizedWhenAnonymous() throws Exception {
		// when & then
		mockMvc.perform(get("/test/auth-member")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.data").value("C-005"))
			.andDo(print());
	}

	@Test
	@DisplayName("인증 정보 부재 시 401 Unauthorized 예외 발생")
	void shouldThrowUnauthorizedWhenNoContext() throws Exception {
		// SecurityContext 비우기
		SecurityContextHolder.clearContext();

		// when & then
		mockMvc.perform(get("/test/auth-member")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andDo(print());
	}

	// 1. 테스트를 위한 임시 컨트롤러 생성
	@TestConfiguration
	static class TestConfig {
		@RestController
		static class TestController {
			@GetMapping("/test/auth-member")
			public String testEndpoint(@AuthMember Member member) {
				return "Resolved: " + member.getEmail();
			}
		}
	}
}
