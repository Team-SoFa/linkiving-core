package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
public class SummaryQueryServiceTest {
	@InjectMocks
	private SummaryQueryService summaryQueryService;

	@Mock
	private SummaryRepository summaryRepository;

	@Test
	@DisplayName("요약 정보 조회 성공")
	void shouldReturnSummaryWhenSummaryExists() {
		// given
		Long linkId = 1L;
		Summary mockSummary = mock(Summary.class); // Summary 엔티티 Mock
		given(summaryRepository.findById(linkId)).willReturn(Optional.of(mockSummary));

		// when
		Summary result = summaryQueryService.getSummary(linkId);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(mockSummary);
		verify(summaryRepository).findById(linkId);
	}

	@Test
	@DisplayName("요약 정보를 찾을 수 없을 때 BusinessException(SUMMARY_NOT_FOUND) 발생")
	void shouldThrowBusinessExceptionWhenSummaryNotFound() {
		// given
		Long linkId = 999L;
		given(summaryRepository.findById(linkId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> summaryQueryService.getSummary(linkId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.SUMMARY_NOT_FOUND);

		verify(summaryRepository).findById(linkId);
	}
}
