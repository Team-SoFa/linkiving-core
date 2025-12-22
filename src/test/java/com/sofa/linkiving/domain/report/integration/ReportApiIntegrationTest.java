package com.sofa.linkiving.domain.report.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.domain.report.dto.request.ReportReq;
import com.sofa.linkiving.domain.report.entity.Report;
import com.sofa.linkiving.domain.report.repository.ReportRepository;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class ReportApiIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private MemberRepository memberRepository;

	private Member testMember;
	private UserDetails testUserDetails;

	@BeforeEach
	void setUp() {
		testMember = memberRepository.save(Member.builder()
			.email("reportUser@test.com")
			.password("password")
			.build());

		testUserDetails = new CustomMemberDetail(testMember, Role.USER);
	}

	@Test
	@DisplayName("제보 생성 요청 시 DB에 저장되고 204 No Content를 반환한다")
	void shouldCreateReportSuccessfully() throws Exception {
		// given
		ReportReq requestDto = new ReportReq("잘못된 링크 정보를 수정해주세요.");

		// when & then
		mockMvc.perform(
				post("/v1/report")
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(requestDto))
			)
			.andExpect(status().isOk());

		List<Report> reports = reportRepository.findAll();
		assertThat(reports).hasSize(1);
		assertThat(reports.get(0).getContent()).isEqualTo("잘못된 링크 정보를 수정해주세요.");
		assertThat(reports.get(0).getMember().getId()).isEqualTo(testMember.getId());
	}
}
