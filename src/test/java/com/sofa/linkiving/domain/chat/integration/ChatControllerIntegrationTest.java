package com.sofa.linkiving.domain.chat.integration;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.chat.dto.response.ChatsRes;
import com.sofa.linkiving.domain.chat.dto.response.ChatsRes.ChatSummary;
import com.sofa.linkiving.domain.chat.facade.ChatFacade;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class ChatControllerIntegrationTest {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private MemberRepository memberRepository;

	@MockitoBean
	private ChatFacade chatFacade;
	@MockitoBean
	private RedisService redisService;

	private Member testMember;
	private UserDetails testUserDetails;

	@BeforeEach
	void setUp() {
		testMember = memberRepository.save(Member.builder()
			.email("list@test.com")
			.password("password")
			.build());
		testUserDetails = new CustomMemberDetail(testMember, Role.USER);
	}

	@Test
	@DisplayName("채팅방 목록 조회 API 호출 및 성공 응답 반환")
	void shouldReturnChatListWhenGetChats() throws Exception {
		// given
		ChatsRes mockResponse = new ChatsRes(List.of(
			new ChatSummary(1L, "Chat 1"),
			new ChatSummary(2L, "Chat 2")
		));

		given(chatFacade.getChats(any(Member.class))).willReturn(mockResponse);

		// when & then
		mockMvc.perform(get("/v1/chats")
				.with(user(testUserDetails)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.chats").isArray())
			.andExpect(jsonPath("$.data.chats[0].title").value("Chat 1"));
	}
}
