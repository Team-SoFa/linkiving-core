package com.sofa.linkiving.domain.report.service;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService 단위 테스트")
public class ReportServiceTest {
	@InjectMocks
	private ReportService reportService;

	@Mock
	private ReportCommandService reportCommandService;

	@Test
	@DisplayName("제보 생성 요청 시 CommandService에게 저장을 위임한다")
	void shouldDelegateToCommandService() {
		// given
		Member member = mock(Member.class);
		String content = "불건전한 링크가 포함되어 있습니다.";

		// when
		reportService.create(member, content);

		// then
		verify(reportCommandService, times(1)).save(member, content);
	}
}
