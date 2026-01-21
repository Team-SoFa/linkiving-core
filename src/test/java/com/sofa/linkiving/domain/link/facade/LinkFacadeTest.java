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
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.link.abstraction.ImageUploader;
import com.sofa.linkiving.domain.link.ai.SummaryClient;
import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.dto.internal.LinksDto;
import com.sofa.linkiving.domain.link.dto.internal.OgTagDto;
import com.sofa.linkiving.domain.link.dto.response.LinkCardsRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.dto.response.MetaScrapeRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RegenerateSummaryRes;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.event.LinkCreatedEvent;
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
	private SummaryService summaryService;

	@Mock
	private OgTagCrawler ogTagCrawler;

	@Mock
	private ImageUploader imageUploader;

	@Mock
	private SummaryClient summaryClient;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@BeforeEach
	void setUp() {
		linkFacade = new LinkFacade(linkService, ogTagCrawler, summaryService, imageUploader, eventPublisher,
			summaryClient);
	}

	@Test
	@DisplayName("메타데이터 크롤링 성공 시 정상적으로 MetaScrapeRes를 반환한다")
	void shouldReturnMetaScrapeResWhenCrawlSucceeds() {
		// given
		String url = "https://velog.io/@jjeongdong/%EB%8F%99%EC%8B%9C%EC%84%B1-%EC%A0%9C%EC%96%B4";
		String originalImageUrl = "https://velog.io/images/thumbnail.png";
		String storedImageUrl = "https://s3-bucket.com/links/uuid.png";
		OgTagDto mockOgTag = OgTagDto.builder()
			.title("동시성 제어")
			.description("동시성 제어에 대한 설명")
			.image(originalImageUrl)
			.url(url)
			.build();

		given(ogTagCrawler.crawl(url)).willReturn(mockOgTag);
		given(imageUploader.uploadFromUrl(originalImageUrl)).willReturn(storedImageUrl);

		// when
		MetaScrapeRes result = linkFacade.scrapeMetadata(url);

		// then
		assertThat(result).isNotNull();
		assertThat(result.title()).isEqualTo("동시성 제어");
		assertThat(result.description()).isEqualTo("동시성 제어에 대한 설명");
		assertThat(result.image()).isEqualTo(storedImageUrl);
		assertThat(result.url()).isEqualTo(url);
		verify(ogTagCrawler, times(1)).crawl(url);
		verify(imageUploader, times(1)).uploadFromUrl(originalImageUrl);
	}

	@Test
	@DisplayName("요약 재생성 및 비교 분석 성공 테스트")
	void shouldReturnRecreateSummaryResponseWhenRecreateSummary() {
		// given
		Long linkId = 1L;
		Long memberId = 1L;

		Member member = mock(Member.class);
		given(member.getId()).willReturn(memberId);

		Format format = Format.DETAILED;
		String url = "https://example.com";
		String existingSummary = "기존 요약 내용입니다.";
		String newSummary = "새로운 상세 요약 내용입니다.";
		String difference = "기존 대비 상세 내용이 추가되었습니다.";

		// 1. LinkService Mocking (URL 가져오기)
		Link mockLink = mock(Link.class);
		given(mockLink.getUrl()).willReturn(url);
		given(linkService.getLink(linkId, member)).willReturn(mockLink);

		// 2. SummaryService (기존 요약 가져오기)
		Summary mockSummary = mock(Summary.class);
		given(mockSummary.getContent()).willReturn(existingSummary);
		given(summaryService.getSummary(linkId)).willReturn(mockSummary);

		// 3. SummaryService (새 요약 생성 및 비교)
		RagRegenerateSummaryRes ragRes = new RagRegenerateSummaryRes(newSummary, difference);
		given(summaryClient.regenerateSummary(linkId, member.getId(), url, existingSummary)).willReturn(ragRes);

		// when
		RegenerateSummaryRes response = linkFacade.recreateSummary(member, linkId, format);

		// then
		assertThat(response).isNotNull();
		assertThat(response.existingSummary()).isEqualTo(existingSummary);
		assertThat(response.newSummary()).isEqualTo(newSummary);
		assertThat(response.difference()).isEqualTo(difference);

		// verify
		verify(summaryService).getSummary(linkId);
		verify(summaryClient).regenerateSummary(linkId, member.getId(), url, existingSummary);
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
		verify(imageUploader, never()).uploadFromUrl(any());
		verify(imageUploader, never()).resolveStoredUrl(any());
	}

	@Test
	@DisplayName("링크 생성 시 이미지 업로드 및 링크가 저장되고, 비동기 요약을 위한 이벤트가 발행된다")
	void shouldCreateLink() {
		// given
		Long linkId = 1L;
		String url = "https://example.com";
		String title = "테스트 제목";
		String memo = "테스트 메모";
		String originalImageUrl = "https://original.com/image.jpg";
		String storedImageUrl = "https://s3-bucket.com/stored-image.jpg";

		Member member = mock(Member.class);
		given(member.getId()).willReturn(100L);
		given(linkQueryService.existsByUrl(eq(member), eq(url))).willReturn(false);
		given(imageUploader.uploadFromUrl(originalImageUrl)).willReturn(storedImageUrl);

		Link savedLink = Link.builder()
			.member(member)
			.url(url)
			.title(title)
			.memo(memo)
			.imageUrl(storedImageUrl)
			.build();
		ReflectionTestUtils.setField(savedLink, "id", linkId);

		given(linkCommandService.saveLink(eq(member), eq(url), eq(title), eq(memo), eq(storedImageUrl)))
			.willReturn(savedLink);

		// when
		LinkRes result = linkFacade.createLink(member, url, title, memo, originalImageUrl);

		// then
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(linkId);

		verify(imageUploader, times(1)).uploadFromUrl(originalImageUrl);
		verify(linkQueryService, times(1)).existsByUrl(member, url);
		verify(linkCommandService, times(1)).saveLink(any(), any(), any(), any(), any());
		verify(eventPublisher, times(1)).publishEvent(any(LinkCreatedEvent.class));
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
		given(linkCommandService.updateLink(any(), any(), any(), any())).willReturn(updatedLink);

		// when
		Link result = linkService.updateLink(1L, member, "수정", null, null);

		// then
		assertThat(result).isEqualTo(updatedLink);
		assertThat(result.getTitle()).isEqualTo("수정");
	}

	@Test
	@DisplayName("링크 제목만 수정할 수 있다")
	void shouldUpdateTitle() {
		// given
		Member member = Member.builder().email("test@example.com").build();
		Link originalLink = Link.builder()
			.member(member)
			.title("원본")
			.memo("메모")
			.build();
		Link updatedLink = Link.builder()
			.member(member)
			.title("수정")
			.memo("메모")
			.build();

		given(linkQueryService.findById(1L, member)).willReturn(originalLink);
		given(linkCommandService.updateLink(any(), eq("수정"), eq("메모"), isNull())).willReturn(updatedLink);

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
		given(linkCommandService.updateLink(any(), eq("제목"), eq("수정"), isNull())).willReturn(updatedLink);

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
	@DisplayName("링크 카드 목록을 조회하고 Response DTO로 변환한다 (페이징 포함)")
	void shouldGetLinkCards() {
		// given
		Member member = mock(Member.class);
		int size = 10;

		Link link1 = Link.builder()
			.member(member)
			.url("https://url1.com")
			.title("Title1")
			.imageUrl("img1.jpg")
			.build();
		Summary summary1 = Summary.builder()
			.link(link1)
			.content("Summary1")
			.build();

		Link link2 = Link.builder()
			.member(member)
			.url("https://url2.com")
			.title("Title2")
			.imageUrl("img2.jpg")
			.build();
		Summary summary2 = Summary.builder()
			.link(link2)
			.content("Summary2")
			.build();

		Link.builder()
			.member(member)
			.url("https://url3.com")
			.title("Title3")
			.imageUrl("img3.jpg")
			.build();

		LinkDto dto1 = new LinkDto(link1, summary1);
		LinkDto dto2 = new LinkDto(link2, summary2);

		LinksDto linksDto = new LinksDto(List.of(dto1, dto2), true);

		given(linkService.getLinksWithSummary(member, null, size)).willReturn(linksDto);

		// when
		LinkCardsRes result = linkFacade.getLinkCards(member, null, size);

		// then
		assertThat(result).isNotNull();

		assertThat(result.hasNext()).isTrue();

		assertThat(result.links()).hasSize(2);

		assertThat(result.links().get(0).title()).isEqualTo("Title1");
		assertThat(result.links().get(0).summary()).isEqualTo("Summary1");

		assertThat(result.links().get(1).title()).isEqualTo("Title2");
		assertThat(result.links().get(1).summary()).isEqualTo("Summary2");
	}

	@Test
	@DisplayName("링크 목록이 비어있을 경우 lastId는 null을 반환한다")
	void shouldReturnNullLastIdWhenListIsEmpty() {
		// given
		Member member = mock(Member.class);
		Long lastId = null;
		int size = 10;

		// 빈 리스트 반환 설정
		LinksDto emptyLinksDto = new LinksDto(List.of(), false);

		given(linkService.getLinksWithSummary(member, lastId, size)).willReturn(emptyLinksDto);

		// when
		LinkCardsRes result = linkFacade.getLinkCards(member, lastId, size);

		// then
		assertThat(result.links()).isEmpty();
		assertThat(result.hasNext()).isFalse();
		assertThat(result.lastId()).isNull();
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
