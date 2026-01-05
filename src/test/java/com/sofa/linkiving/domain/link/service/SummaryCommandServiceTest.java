package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryCommandService 단위 테스트")
public class SummaryCommandServiceTest {

	@InjectMocks
	private SummaryCommandService summaryCommandService;

	@Mock
	private SummaryRepository summaryRepository;

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

	@Test
	@DisplayName("요약를 저장할 수 있다")
	void shouldSaveLink() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("테스트 링크")
			.memo("메모")
			.imageUrl("https://example.com/image.jpg")
			.build();

		Summary summary = Summary.builder()
			.format(Format.CONCISE)
			.link(link)
			.content("요약")
			.build();

		given(summaryRepository.save(any(Summary.class))).willReturn(summary);

		// when
		Summary save = summaryCommandService.save(
			link,
			Format.CONCISE,
			"요약"
		);

		// then
		assertThat(save).isNotNull();
		assertThat(save.getContent()).isEqualTo("요약");
		verify(summaryRepository, times(1)).save(any(Summary.class));
	}
}
