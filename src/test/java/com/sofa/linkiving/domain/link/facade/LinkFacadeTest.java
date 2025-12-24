package com.sofa.linkiving.domain.link.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sofa.linkiving.domain.link.dto.OgTagDto;
import com.sofa.linkiving.domain.link.dto.response.MetaScrapeRes;
import com.sofa.linkiving.domain.link.dto.response.RecreateSummaryResponse;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.service.LinkCommandService;
import com.sofa.linkiving.domain.link.service.LinkQueryService;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;
import com.sofa.linkiving.domain.link.util.OgTagCrawler;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkFacade 단위 테스트")
public class LinkFacadeTest {

	private LinkFacade linkFacade;

	@InjectMocks
	private LinkService linkService;

	@Mock
	private LinkCommandService linkCommandService;

	@Mock
	private LinkQueryService linkQueryService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private SummaryService summaryService;

	@Mock
	private OgTagCrawler ogTagCrawler;

	@BeforeEach
	void setUp() {
		linkFacade = new LinkFacade(linkService, ogTagCrawler, summaryService);
	}

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
		Member member = mock(Member.class);
		String url = "https://example.com";
		Link link = mock(Link.class);

		given(link.getUrl()).willReturn(url);
		given(linkService.getLink(linkId, member)).willReturn(link); // Service Mock 동작 정의

		Summary mockSummary = mock(Summary.class);
		given(mockSummary.getContent()).willReturn("Old");
		given(summaryService.getSummary(linkId)).willReturn(mockSummary);
		given(summaryService.createSummary(linkId, url, Format.DETAILED)).willReturn("New");
		given(summaryService.comparisonSummary("Old", "New")).willReturn("Diff");

		// when
		RecreateSummaryResponse res = linkFacade.recreateSummary(member, linkId, Format.DETAILED);

		// then
		assertThat(res.newSummary()).isEqualTo("New");
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

	@Test
	@DisplayName("링크를 생성하고 Entity를 반환한다")
	void shouldCreateLink() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		Link link = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("테스트 링크")
			.build();

		given(linkQueryService.existsByUrl(member, "https://example.com")).willReturn(false);
		given(linkCommandService.saveLink(any(), any(), any(), any(), any())).willReturn(link);

		// when
		Link createdLink = linkService.createLink(
			member, "https://example.com", "테스트 링크", "메모", null
		);

		// then
		assertThat(createdLink).isNotNull();
		assertThat(createdLink).isInstanceOf(Link.class); // Entity 반환 확인
		assertThat(createdLink.getUrl()).isEqualTo("https://example.com");

		verify(linkQueryService, times(1)).existsByUrl(member, "https://example.com");
		verify(linkCommandService, times(1)).saveLink(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("중복된 URL로 링크 생성 시 예외가 발생한다")
	void shouldThrowExceptionWhenDuplicateUrl() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		given(linkQueryService.existsByUrl(member, "https://example.com")).willReturn(true);

		// when & then
		assertThatThrownBy(() -> linkService.createLink(
			member, "https://example.com", "테스트 링크", null, null
		))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.DUPLICATE_URL);

		verify(linkCommandService, never()).saveLink(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("링크를 수정하고 Entity를 반환한다")
	void shouldUpdateLink() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		Link originalLink = Link.builder().member(member).url("https://example.com").title("원본").build();
		Link updatedLink = Link.builder().member(member).url("https://example.com").title("수정").build();

		given(linkQueryService.findById(1L, member)).willReturn(originalLink);
		given(linkCommandService.updateLink(any(), any(), any())).willReturn(updatedLink);

		// when
		Link result = linkService.updateLink(1L, member, "수정", null);

		// then
		assertThat(result).isEqualTo(updatedLink);
		assertThat(result.getTitle()).isEqualTo("수정");
	}

	@Test
	@DisplayName("링크 제목만 수정할 수 있다")
	void shouldUpdateTitle() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		Link originalLink = Link.builder().member(member).title("원본").memo("메모").build();
		Link updatedLink = Link.builder().member(member).title("수정").memo("메모").build();

		given(linkQueryService.findById(1L, member)).willReturn(originalLink);
		given(linkCommandService.updateLink(any(), eq("수정"), eq("메모"))).willReturn(updatedLink);

		// when
		Link result = linkService.updateTitle(1L, member, "수정");

		// then
		assertThat(result.getTitle()).isEqualTo("수정");
	}

	@Test
	@DisplayName("링크 메모만 수정할 수 있다")
	void shouldUpdateMemo() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		Link originalLink = Link.builder().member(member).title("제목").memo("원본").build();
		Link updatedLink = Link.builder().member(member).title("제목").memo("수정").build();

		given(linkQueryService.findById(1L, member)).willReturn(originalLink);
		given(linkCommandService.updateLink(any(), eq("제목"), eq("수정"))).willReturn(updatedLink);

		// when
		Link result = linkService.updateMemo(1L, member, "수정");

		// then
		assertThat(result.getMemo()).isEqualTo("수정");
	}

	@Test
	@DisplayName("링크를 삭제할 수 있다")
	void shouldDeleteLink() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		Link link = Link.builder().member(member).build();

		given(linkQueryService.findById(1L, member)).willReturn(link);

		// when
		linkService.deleteLink(1L, member);

		// then
		verify(linkCommandService, times(1)).deleteLink(link);
	}

	@Test
	@DisplayName("단일 링크 Entity를 조회할 수 있다")
	void shouldGetLink() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		Link link = Link.builder().member(member).url("https://example.com").build();

		given(linkQueryService.findById(1L, member)).willReturn(link);

		// when
		Link result = linkService.getLink(1L, member);

		// then
		assertThat(result).isEqualTo(link);
		verify(linkQueryService, times(1)).findById(1L, member);
	}

	@Test
	@DisplayName("링크 Entity 페이지 목록을 조회할 수 있다")
	void shouldGetLinkList() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		Link link1 = Link.builder().member(member).title("1").build();
		Link link2 = Link.builder().member(member).title("2").build();
		Pageable pageable = PageRequest.of(0, 10);
		Page<Link> expectedPage = new PageImpl<>(List.of(link1, link2));

		given(linkQueryService.findAllByMember(member, pageable)).willReturn(expectedPage);

		// when
		Page<Link> result = linkService.getLinkList(member, pageable);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.getContent().get(0)).isInstanceOf(Link.class);
	}

	@Test
	@DisplayName("URL로 링크 ID를 찾을 수 있다 (중복 체크용)")
	void shouldFindLinkIdByUrl() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		given(linkQueryService.findIdByUrl(member, "https://example.com"))
			.willReturn(Optional.of(123L));

		// when
		Optional<Long> result = linkService.findLinkIdByUrl(member, "https://example.com");

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(123L);
	}
}
