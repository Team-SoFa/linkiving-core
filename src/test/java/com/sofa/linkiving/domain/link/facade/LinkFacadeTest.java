package com.sofa.linkiving.domain.link.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.dto.response.RecreateSummaryResponse;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
public class LinkFacadeTest {

	@InjectMocks
	private LinkFacade linkFacade;

	@Mock
	private LinkService linkService;

	@Mock
	private SummaryService summaryService;

	@Test
	@DisplayName("요약 재생성 및 비교 분석 성공 테스트")
	void shouldReturnRecreateSummaryResponseWhenRecreateSummary() {
		// given
		Long linkId = 1L;
		Member member = mock(Member.class); // Member 객체 Mock
		Format format = Format.DETAILED;
		String url = "https://example.com";
		String existingSummaryBody = "기존 요약 내용입니다.";
		String newSummaryBody = "새로운 상세 요약 내용입니다.";
		String comparisonBody = "기존 대비 상세 내용이 추가되었습니다.";

		// 1. LinkService Mocking (URL 가져오기)
		LinkRes mockLinkRes = mock(LinkRes.class);
		given(mockLinkRes.url()).willReturn(url);
		given(linkService.getLink(linkId, member)).willReturn(mockLinkRes);

		// 2. SummaryService (기존 요약 가져오기)
		Summary mockSummary = mock(Summary.class);
		given(mockSummary.getContent()).willReturn(existingSummaryBody);
		given(summaryService.getSummary(linkId)).willReturn(mockSummary);

		// 3. SummaryService (새 요약 생성 및 비교)
		given(summaryService.createSummary(linkId, url, format)).willReturn(newSummaryBody);
		given(summaryService.comparisonSummary(existingSummaryBody, newSummaryBody)).willReturn(comparisonBody);

		// when
		RecreateSummaryResponse response = linkFacade.recreateSummary(member, linkId, format);

		// then
		assertThat(response).isNotNull();
		assertThat(response.existingSummary()).isEqualTo(existingSummaryBody);
		assertThat(response.newSummary()).isEqualTo(newSummaryBody);
		assertThat(response.comparison()).isEqualTo(comparisonBody);

		// verify
		verify(linkService).getLink(linkId, member);
		verify(summaryService).getSummary(linkId);
		verify(summaryService).createSummary(linkId, url, format);
		verify(summaryService).comparisonSummary(existingSummaryBody, newSummaryBody);
	}
}
