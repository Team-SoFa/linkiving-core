package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryQueryService 단위 테스트")
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
		given(summaryRepository.findByLinkIdAndSelectedTrue(linkId)).willReturn(Optional.of(mockSummary));

		// when
		Summary result = summaryQueryService.getSummary(linkId);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(mockSummary);
		verify(summaryRepository).findByLinkIdAndSelectedTrue(linkId);
	}

	@Test
	@DisplayName("요약 정보를 찾을 수 없을 때 BusinessException(SUMMARY_NOT_FOUND) 발생")
	void shouldThrowBusinessExceptionWhenSummaryNotFound() {
		// given
		Long linkId = 999L;
		given(summaryRepository.findByLinkIdAndSelectedTrue(linkId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> summaryQueryService.getSummary(linkId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.SUMMARY_NOT_FOUND);

		verify(summaryRepository).findByLinkIdAndSelectedTrue(linkId);
	}

	@Test
	@DisplayName("링크 리스트가 비어있으면 빈 Map을 반환하고 리포지토리를 호출하지 않음")
	void shouldReturnEmptyMapWhenLinksListIsEmpty() {
		// given
		List<Link> emptyLinks = Collections.emptyList();

		// when
		Map<Long, Summary> result = summaryQueryService.getSelectedSummariesByLinks(emptyLinks);

		// then
		assertThat(result).isEmpty();
		verify(summaryRepository, never()).findAllByLinkInAndSelectedTrue(any());
	}

	@Test
	@DisplayName("링크 리스트로 선택된 요약들을 조회하여 Map으로 변환함")
	void shouldGetSelectedSummariesByLinks() {
		// given
		Link link1 = mock(Link.class);
		given(link1.getId()).willReturn(1L);

		Link link2 = mock(Link.class);
		given(link2.getId()).willReturn(2L);

		Summary summary1 = mock(Summary.class);
		given(summary1.getLink()).willReturn(link1);

		Summary summary2 = mock(Summary.class);
		given(summary2.getLink()).willReturn(link2);

		List<Link> links = List.of(link1, link2);
		List<Summary> summaries = List.of(summary1, summary2);

		given(summaryRepository.findAllByLinkInAndSelectedTrue(links)).willReturn(summaries);

		// when
		Map<Long, Summary> result = summaryQueryService.getSelectedSummariesByLinks(links);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(1L)).isEqualTo(summary1);
		assertThat(result.get(2L)).isEqualTo(summary2);
	}
}
