package com.sofa.linkiving.domain.link.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryWorkerFacade 단위 테스트")
class SummaryWorkerFacadeTest {

	@InjectMocks
	private SummaryWorkerFacade summaryWorkerFacade;

	@Mock
	private LinkService linkService;

	@Mock
	private SummaryService summaryService;

	@Test
	@DisplayName("회원 정보를 포함하여 링크를 정상적으로 조회함")
	void shouldGetLinkWithMember() {
		// given
		Long linkId = 1L;
		Link mockLink = mock(Link.class);
		given(linkService.getLinkWithMember(linkId)).willReturn(mockLink);

		// when
		Link result = summaryWorkerFacade.getLinkWithMember(linkId);

		// then
		assertThat(result).isEqualTo(mockLink);
		verify(linkService, times(1)).getLinkWithMember(linkId);
	}

	@Test
	@DisplayName("링크의 요약 상태를 업데이트함")
	void shouldUpdateSummaryStatus() {
		// given
		Long linkId = 1L;
		SummaryStatus status = SummaryStatus.FAILED;

		// when
		summaryWorkerFacade.updateSummaryStatus(linkId, status);

		// then
		verify(linkService, times(1)).updateSummaryStatus(linkId, status);
	}

	@Test
	@DisplayName("상태가 PROCESSING인 경우 요약 생성 후 COMPLETED 상태로 업데이트함")
	void shouldCreateSummaryAndUpdateStatusToCompleted_WhenStatusIsProcessing() {
		// given
		Long linkId = 1L;
		String summaryContent = "성공적인 초기 요약 내용";
		Link mockLink = mock(Link.class);
		Summary mockSummary = mock(Summary.class);

		given(linkService.getLink(linkId)).willReturn(mockLink);
		given(mockLink.getSummaryStatus()).willReturn(SummaryStatus.PROCESSING);
		given(summaryService.createInitialSummary(mockLink, summaryContent)).willReturn(mockSummary);

		// when
		Summary result = summaryWorkerFacade.createInitialSummaryAndUpdateStatus(linkId, summaryContent);

		// then
		assertThat(result).isEqualTo(mockSummary); // 정상 생성된 요약 반환 확인
		verify(summaryService, times(1)).createInitialSummary(mockLink, summaryContent);
		verify(mockLink, times(1)).updateSummaryStatus(SummaryStatus.COMPLETED);
	}

	@Test
	@DisplayName("상태가 PROCESSING이 아니면 요약을 무시하고 null을 반환함")
	void shouldReturnNullAndNotCreateSummary_WhenStatusIsNotProcessing() {
		// given
		Long linkId = 1L;
		String summaryContent = "무시되어야 할 요약 내용";
		Link mockLink = mock(Link.class);

		given(linkService.getLink(linkId)).willReturn(mockLink);
		given(mockLink.getSummaryStatus()).willReturn(SummaryStatus.PENDING);

		// when
		Summary result = summaryWorkerFacade.createInitialSummaryAndUpdateStatus(linkId, summaryContent);

		// then
		assertThat(result).isNull();
		verify(summaryService, never()).createInitialSummary(any(), any());
		verify(mockLink, never()).updateSummaryStatus(any());
	}
}
