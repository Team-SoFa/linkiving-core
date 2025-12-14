package com.sofa.linkiving.domain.link.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.dto.OgTagDto;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.dto.response.MetaScrapeRes;
import com.sofa.linkiving.domain.link.dto.response.RecreateSummaryResponse;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;
import com.sofa.linkiving.domain.link.util.OgTagCrawler;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkFacade 단위 테스트")
public class LinkFacadeTest {

	@InjectMocks
	private LinkFacade linkFacade;

	@Mock
	private LinkService linkService;

	@Mock
	private SummaryService summaryService;

	@Mock
	private OgTagCrawler ogTagCrawler;

	@Test
	@DisplayName("메타데이터 크롤링 성공 시 정상적으로 MetaScrapeRes를 반환한다")
	void shouldReturnMetaScrapeResWhenCrawlSucceeds() {
		// given
		String url = "https://velog.io/@jjeongdong/%EB%8F%99%EC%8B%9C%EC%84%B1-%EC%A0%9C%EC%96%B4";
		OgTagDto mockOgTag = OgTagDto.builder()
			.title("동시성 제어")
			.description("동시성 제어에 대한 설명")
			.image("https://velog.io/images/thumbnail.png")
			.url(url)
			.build();

		given(ogTagCrawler.crawl(url)).willReturn(mockOgTag);

		// when
		MetaScrapeRes result = linkFacade.scrapeMetadata(url);

		// then
		assertThat(result).isNotNull();
		assertThat(result.title()).isEqualTo("동시성 제어");
		assertThat(result.description()).isEqualTo("동시성 제어에 대한 설명");
		assertThat(result.image()).isEqualTo("https://velog.io/images/thumbnail.png");
		assertThat(result.url()).isEqualTo(url);
		verify(ogTagCrawler, times(1)).crawl(url);
	}

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

	@Test
	@DisplayName("메타데이터 크롤링 실패 시 빈 값으로 MetaScrapeRes를 반환한다")
	void shouldReturnEmptyMetaScrapeResWhenCrawlFails() {
		// given
		String url = "https://invalid-url.com";
		given(ogTagCrawler.crawl(url)).willReturn(OgTagDto.EMPTY);

		// when
		MetaScrapeRes result = linkFacade.scrapeMetadata(url);

		// then
		assertThat(result).isNotNull();
		assertThat(result.title()).isEmpty();
		assertThat(result.description()).isEmpty();
		assertThat(result.image()).isEmpty();
		assertThat(result.url()).isEmpty();
		verify(ogTagCrawler, times(1)).crawl(url);
	}
}
