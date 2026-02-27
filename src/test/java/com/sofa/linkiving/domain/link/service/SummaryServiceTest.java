package com.sofa.linkiving.domain.link.service;

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
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
public class SummaryServiceTest {
	@InjectMocks
	private SummaryService summaryService;

	@Mock
	private SummaryQueryService summaryQueryService;

	@Mock
	private SummaryCommandService summaryCommandService;

	@Test
	@DisplayName("요약를 생성할 수 있다")
	void shouldCreateLink() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("테스트 링크")
			.build();

		Summary summary = Summary.builder()
			.format(Format.CONCISE)
			.link(link)
			.content("요약")
			.build();

		given(summaryCommandService.save(any(), any(), any())).willReturn(summary);

		// when
		Summary save = summaryCommandService.save(
			link,
			Format.CONCISE,
			"요약"
		);

		// then
		assertThat(save).isNotNull();
		assertThat(save.getContent()).isEqualTo("요약");
		verify(summaryCommandService, times(1)).save(any(), any(), any());
	}

	@Test
	@DisplayName("getSummary 호출 시 SummaryQueryService에게 위임한다")
	void shouldCallGetSummaryWhenGetSummary() {
		// given
		Long linkId = 1L;
		Summary mockSummary = mock(Summary.class);

		given(summaryQueryService.getSummary(linkId)).willReturn(mockSummary);

		// when
		Summary result = summaryService.getSummary(linkId);

		// then
		assertThat(result).isEqualTo(mockSummary);
		verify(summaryQueryService).getSummary(linkId);
	}

	@Test
	@DisplayName("createInitialSummary: Format.CONCISE 형태로 초기 요약을 생성하고 저장한다")
	void shouldCreateInitialSummaryWithConciseFormat() {
		// given
		Link link = mock(Link.class);
		String content = "테스트 초기 요약 내용";
		Summary expectedSummary = mock(Summary.class);

		given(summaryCommandService.initialSave(link, Format.CONCISE, content)).willReturn(expectedSummary);

		// when
		Summary actualSummary = summaryService.createInitialSummary(link, content);

		// then
		assertThat(actualSummary).isEqualTo(expectedSummary);
		verify(summaryCommandService, times(1)).initialSave(link, Format.CONCISE, content);
	}
}
