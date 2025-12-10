package com.sofa.linkiving.domain.chat.integration;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.domain.chat.ai.AiTitleClient;
import com.sofa.linkiving.domain.chat.dto.request.CreateChatReq;
import com.sofa.linkiving.domain.chat.service.FeedbackService;
import com.sofa.linkiving.domain.chat.service.MessageService;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChatControllerIntegrationTest {

	private static final String BASE_URL = "/v1/chats";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AiTitleClient aiTitleClient;

	@MockitoBean
	private RedisService redisService;

	@MockitoBean
	private MessageService messageService;

	@MockitoBean
	private FeedbackService feedbackService;

	private UserDetails testUserDetails;

	@BeforeEach
	void setUp() {
		Member testMember = memberRepository.save(Member.builder()
			.email("chatuser@test.com")
			.password("password")
			.build());

		testUserDetails = new CustomMemberDetail(testMember, Role.USER);
	}

	@Test
	@DisplayName("유효한 요청 시 채팅 생성 및 200 OK 반환")
	void shouldCreateChatSuccessfullyWhenValidRequest() throws Exception {
		// given
		String firstChatContent = "AI 관련 최신 뉴스 알려줘";
		CreateChatReq req = new CreateChatReq(firstChatContent);
		String title = aiTitleClient.generateSummary(firstChatContent);

		// when & then
		mockMvc.perform(post(BASE_URL)
				.with(csrf())
				.with(user(testUserDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.title").value(title))
			.andExpect(jsonPath("$.data.firstChat").value(firstChatContent));
	}

	@Test
	@DisplayName("첫 대화 내용 누락 시 400 Bad Request 반환")
	void shouldReturnBadRequestWhenFirstChatIsBlank() throws Exception {
		// given
		CreateChatReq req = new CreateChatReq("");

		// when & then
		mockMvc.perform(post(BASE_URL)
				.with(csrf())
				.with(user(testUserDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}
}
