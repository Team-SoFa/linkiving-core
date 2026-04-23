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
import com.sofa.linkiving.domain.chat.dto.request.UpsertFeedbackReq;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeedbackIntegrationTest {

	private static final String BASE_URL = "/v1/messages";

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
	private FeedbackRepository feedbackRepository;

	@MockitoBean
	private RedisService redisService;

	private UserDetails testUserDetails;
	private Message testMessage;

	@BeforeEach
	void setUp() {
		Member member = memberRepository.save(Member.builder()
			.email("feedback_user@test.com")
			.password("password")
			.build());
		testUserDetails = new CustomMemberDetail(member, Role.USER);

		Chat chat = chatRepository.save(Chat.builder()
			.member(member)
			.title("Test Chat")
			.build());

		testMessage = messageRepository.save(Message.builder()
			.chat(chat)
			.type(Type.AI)
			.content("AI Response")
			.build());
	}

	@Test
	@DisplayName("새로운 피드백 등록 요청 시 DB에 새로 저장되고 200 OK를 반환함")
	void shouldCreateNewFeedbackAndReturnOkWhenNoExistingFeedback() throws Exception {
		// given
		Long messageId = testMessage.getId();
		UpsertFeedbackReq req = new UpsertFeedbackReq(Sentiment.LIKE, "매우 유용한 답변입니다.");

		// when & then
		mockMvc.perform(put(BASE_URL + "/{messageId}/feedback", messageId)
				.with(csrf())
				.with(user(testUserDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").isNumber());

		assertThat(feedbackRepository.count()).isEqualTo(1);
		Feedback savedFeedback = feedbackRepository.findAll().get(0);
		assertThat(savedFeedback.getMessage().getId()).isEqualTo(messageId);
		assertThat(savedFeedback.getSentiment()).isEqualTo(Sentiment.LIKE);
		assertThat(savedFeedback.getText()).isEqualTo("매우 유용한 답변입니다.");
	}

	@Test
	@DisplayName("기존 피드백이 존재하는 상태에서 등록 요청 시 내용이 수정되고 200 OK를 반환함")
	void shouldUpdateExistingFeedbackAndReturnOkWhenFeedbackExists() throws Exception {
		// given
		Feedback existingFeedback = feedbackRepository.save(Feedback.builder()
			.message(testMessage)
			.sentiment(Sentiment.LIKE)
			.text("기존 피드백입니다.")
			.build());

		Long messageId = testMessage.getId();
		UpsertFeedbackReq req = new UpsertFeedbackReq(Sentiment.DISLIKE, "내용이 수정되었습니다.");

		// when & then
		mockMvc.perform(put(BASE_URL + "/{messageId}/feedback", messageId)
				.with(csrf())
				.with(user(testUserDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(existingFeedback.getId()));

		assertThat(feedbackRepository.count()).isEqualTo(1);
		Feedback updatedFeedback = feedbackRepository.findAll().get(0);
		assertThat(updatedFeedback.getId()).isEqualTo(existingFeedback.getId());
		assertThat(updatedFeedback.getSentiment()).isEqualTo(Sentiment.DISLIKE);
		assertThat(updatedFeedback.getText()).isEqualTo("내용이 수정되었습니다.");
	}

	@Test
	@DisplayName("피드백 상태 누락 시 400 Bad Request를 반환함")
	void shouldReturnBadRequestWhenSentimentIsNull() throws Exception {
		// given
		Long messageId = testMessage.getId();
		UpsertFeedbackReq req = new UpsertFeedbackReq(null, "내용만 있음");

		// when & then
		mockMvc.perform(put(BASE_URL + "/{messageId}/feedback", messageId)
				.with(csrf())
				.with(user(testUserDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("존재하지 않는 메시지에 피드백 등록/수정 시 404 Not Found를 반환함")
	void shouldReturnNotFoundWhenMessageDoesNotExist() throws Exception {
		// given
		Long invalidMessageId = 99999L;
		UpsertFeedbackReq req = new UpsertFeedbackReq(Sentiment.DISLIKE, "별로예요");

		// when & then
		mockMvc.perform(put(BASE_URL + "/{messageId}/feedback", invalidMessageId)
				.with(csrf())
				.with(user(testUserDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false));
	}
}
