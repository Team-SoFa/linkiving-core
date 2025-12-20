package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryCommandService 단위 테스트")
public class SummaryCommandServiceTest {

	@Mock
	private SummaryRepository summaryRepository;

	@InjectMocks
	private SummaryCommandService summaryCommandService;

	@Test
	@DisplayName("요약 선택 변경 성공")
	void shouldSelectSummarySuccessfully() {
		// given
		Long linkId = 1L;
		Long summaryId = 2L;
		given(summaryRepository.clearSelectedByLinkId(linkId)).willReturn(1);
		given(summaryRepository.selectByIdAndLinkId(summaryId, linkId)).willReturn(1);

		// when
		summaryCommandService.selectSummary(linkId, summaryId);

		// then
		verify(summaryRepository).clearSelectedByLinkId(linkId);
		verify(summaryRepository).selectByIdAndLinkId(summaryId, linkId);
	}

	@Test
	@DisplayName("존재하지 않는 요약 선택 시 예외 발생")
	void shouldThrowExceptionWhenSummaryNotFound() {
		// given
		Long linkId = 1L;
		Long summaryId = 999L;
		given(summaryRepository.clearSelectedByLinkId(linkId)).willReturn(1);
		given(summaryRepository.selectByIdAndLinkId(summaryId, linkId)).willReturn(0);

		// when & then
		assertThatThrownBy(() -> summaryCommandService.selectSummary(linkId, summaryId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.SUMMARY_NOT_FOUND);

		verify(summaryRepository).clearSelectedByLinkId(linkId);
		verify(summaryRepository).selectByIdAndLinkId(summaryId, linkId);
	}
}
