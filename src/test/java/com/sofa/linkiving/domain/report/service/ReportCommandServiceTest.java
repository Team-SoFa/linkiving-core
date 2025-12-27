package com.sofa.linkiving.domain.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.report.entity.Report;
import com.sofa.linkiving.domain.report.repository.ReportRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportCommandService 단위 테스트")
public class ReportCommandServiceTest {

	@InjectMocks
	private ReportCommandService reportCommandService;

	@Mock
	private ReportRepository reportRepository;

	@Test
	@DisplayName("제보 내용을 저장할 수 있다")
	void shouldSaveReport() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.build();
		String content = "버그 제보합니다.";

		Report report = Report.builder()
			.member(member)
			.content(content)
			.build();

		given(reportRepository.save(any(Report.class))).willReturn(report);

		// when
		Report savedReport = reportCommandService.save(member, content);

		// then
		assertThat(savedReport).isNotNull();
		assertThat(savedReport.getContent()).isEqualTo(content);
		assertThat(savedReport.getMember()).isEqualTo(member);

		verify(reportRepository, times(1)).save(any(Report.class));
	}
}
