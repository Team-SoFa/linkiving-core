package com.sofa.linkiving.domain.chat.integration;

import static org.assertj.core.api.Assertions.*;
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
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
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
class ChatIntegrationTest {

	private static final String BASE_URL = "/v1/chats";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private AiTitleClient aiTitleClient;

	@MockitoBean
	private RedisService redisService;

	@MockitoBean
	private FeedbackService feedbackService;

	@MockitoBean
	private MessageService messageService;

	private UserDetails testUserDetails;
	private Member testMember;

	@BeforeEach
	void setUp() {
		testMember = memberRepository.save(Member.builder()
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

	@Test
	@DisplayName("채팅방 삭제 API 호출 시 성공 응답 반환")
	void shouldReturnSuccessWhenDeleteChat() throws Exception {
		// given
		Chat chat = chatRepository.save(Chat.builder()
			.member(testMember)
			.title("삭제할 채팅방")
			.build());

		Long chatId = chat.getId();

		// when & then
		mockMvc.perform(delete(BASE_URL + "/{chatId}", chatId)
				.with(csrf())
				.with(user(testUserDetails)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		assertThat(chatRepository.existsById(chatId)).isFalse();
	}

	@Test
	@DisplayName("채팅방 목록 조회 API 호출 및 성공 응답 반환")
	void shouldReturnChatListWhenGetChats() throws Exception {
		// given
		Chat chat2 = chatRepository.save(Chat.builder()
			.member(testMember)
			.title("Chat 2")
			.build());

		// 생성 시간 차이를 두기 위해 잠시 대기 (정렬 테스트)
		Thread.sleep(10);

		Chat chat1 = chatRepository.save(Chat.builder()
			.member(testMember)
			.title("Chat 1")
			.build());

		messageRepository.save(Message.builder()
			.chat(chat1)
			.content("1")
			.type(Type.AI)
			.build()
		);

		messageRepository.save(Message.builder()
			.chat(chat2)
			.content("2")
			.type(Type.USER)
			.build()
		);

		// when & then
		mockMvc.perform(get(BASE_URL)
				.with(user(testUserDetails)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.chats").isArray())
			.andExpect(jsonPath("$.data.chats[0].title").value("Chat 2"))
			.andExpect(jsonPath("$.data.chats[1].title").value("Chat 1"));
	}
}
