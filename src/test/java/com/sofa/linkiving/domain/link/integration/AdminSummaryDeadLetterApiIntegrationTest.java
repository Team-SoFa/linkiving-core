package com.sofa.linkiving.domain.link.integration;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.SummaryDeadLetter;
import com.sofa.linkiving.domain.link.enums.DeadLetterStatus;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.link.repository.SummaryDeadLetterRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.global.util.HashidsUtils;
import com.sofa.linkiving.infra.redis.RedisService;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class AdminSummaryDeadLetterApiIntegrationTest {

	private static final String BASE_URL = "/v1/admin/summary/dead-letters";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private SummaryDeadLetterRepository deadLetterRepository;

	@Autowired
	private HashidsUtils hashidsUtils;

	@MockitoBean
	private RedisService redisService;

	private UserDetails adminDetails;
	private UserDetails userDetails;
	private Member adminMember;

	@BeforeEach
	void setUp() {
		adminMember = memberRepository.save(Member.builder()
			.email("admin@test.com")
			.password("password")
			.build());
		Member normalMember = memberRepository.save(Member.builder()
			.email("user@test.com")
			.password("password")
			.build());
		adminDetails = new CustomMemberDetail(adminMember, Role.ADMIN);
		userDetails = new CustomMemberDetail(normalMember, Role.USER);
	}

	private SummaryDeadLetter saveDeadLetter(Long linkId, DeadLetterStatus status) {
		SummaryDeadLetter deadLetter = SummaryDeadLetter.builder()
			.linkId(linkId)
			.memberId(adminMember.getId())
			.errorCode("E_000")
			.exceptionType("SocketTimeoutException")
			.failureReason("timeout")
			.requestId("req-1")
			.traceId("trace-1")
			.build();
		if (status == DeadLetterStatus.REPROCESSED) {
			deadLetter.markReprocessed();
		} else if (status == DeadLetterStatus.IGNORED) {
			deadLetter.markIgnored();
		}
		return deadLetterRepository.save(deadLetter);
	}

	@Test
	@DisplayName("ADMIN 권한으로 dead-letter 목록을 조회한다")
	void getDeadLetters_asAdmin() throws Exception {
		saveDeadLetter(1L, DeadLetterStatus.PENDING);
		saveDeadLetter(2L, DeadLetterStatus.PENDING);

		mockMvc.perform(get(BASE_URL)
				.with(user(adminDetails)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	@DisplayName("status 필터로 PENDING 상태만 조회한다")
	void getDeadLetters_filterByStatus() throws Exception {
		saveDeadLetter(1L, DeadLetterStatus.PENDING);
		saveDeadLetter(2L, DeadLetterStatus.IGNORED);

		mockMvc.perform(get(BASE_URL)
				.param("status", "PENDING")
				.with(user(adminDetails)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
	}

	@Test
	@DisplayName("USER 권한은 403 Forbidden")
	void getDeadLetters_asUser_forbidden() throws Exception {
		mockMvc.perform(get(BASE_URL)
				.with(user(userDetails)))
			.andDo(print())
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("PENDING dead-letter 재처리 시 REPROCESSED 로 변경된다")
	void reprocess_pending_success() throws Exception {
		Link link = linkRepository.save(Link.builder()
			.member(adminMember)
			.url("https://example.com")
			.title("t")
			.build());
		SummaryDeadLetter deadLetter = saveDeadLetter(link.getId(), DeadLetterStatus.PENDING);
		String hashId = hashidsUtils.encode(deadLetter.getId());

		mockMvc.perform(post(BASE_URL + "/{id}/reprocess", hashId)
				.with(csrf())
				.with(user(adminDetails)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		assertThat(deadLetterRepository.findById(deadLetter.getId()).orElseThrow().getStatus())
			.isEqualTo(DeadLetterStatus.REPROCESSED);
	}

	@Test
	@DisplayName("존재하지 않는 dead-letter 재처리 시 404")
	void reprocess_notFound() throws Exception {
		String hashId = hashidsUtils.encode(999999L);
		mockMvc.perform(post(BASE_URL + "/{id}/reprocess", hashId)
				.with(csrf())
				.with(user(adminDetails)))
			.andDo(print())
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("이미 재처리된 dead-letter 재처리 시 409")
	void reprocess_alreadyReprocessed_conflict() throws Exception {
		SummaryDeadLetter deadLetter = saveDeadLetter(1L, DeadLetterStatus.REPROCESSED);

		String hashId = hashidsUtils.encode(deadLetter.getId());

		mockMvc.perform(post(BASE_URL + "/{id}/reprocess", hashId)
				.with(csrf())
				.with(user(adminDetails)))
			.andDo(print())
			.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("dead-letter 무시 처리 시 IGNORED 로 변경된다")
	void ignore_success() throws Exception {
		SummaryDeadLetter deadLetter = saveDeadLetter(1L, DeadLetterStatus.PENDING);
		String hashId = hashidsUtils.encode(deadLetter.getId());

		mockMvc.perform(post(BASE_URL + "/{id}/ignore", hashId)
				.with(csrf())
				.with(user(adminDetails)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		assertThat(deadLetterRepository.findById(deadLetter.getId()).orElseThrow().getStatus())
			.isEqualTo(DeadLetterStatus.IGNORED);
	}
}
