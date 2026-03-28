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
import com.sofa.linkiving.domain.link.dto.response.LinkDetailRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.dto.response.LinkTotalCountRes;
import com.sofa.linkiving.domain.link.dto.response.MetaScrapeRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RegenerateSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.SummaryRes;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.event.LinkCreatedEvent;
import com.sofa.linkiving.domain.link.event.LinkSyncEvent;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;
import com.sofa.linkiving.domain.link.util.OgTagCrawler;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkFacade 단위 테스트")
class LinkFacadeTest {

	@InjectMocks
	private LinkFacade linkFacade;

	@Mock
	private LinkService linkService;

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

		Link mockLink = mock(Link.class);
		given(mockLink.getUrl()).willReturn(url);
		given(linkService.getLinkForSummaryUpdate(linkId, member)).willReturn(mockLink);

		Summary mockSummary = mock(Summary.class);
		given(mockSummary.getContent()).willReturn(existingSummary);
		given(summaryService.getSummary(linkId)).willReturn(mockSummary);

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
		verify(linkService, times(1)).getLinkForSummaryUpdate(linkId, member);
		verify(summaryService, times(1)).getSummary(linkId);
		verify(summaryClient, times(1)).regenerateSummary(linkId, member.getId(), url, existingSummary);
	}

	@Test
	@DisplayName("요약을 새롭게 생성하고, 해당 요약을 선택(selected=true) 상태로 변경한다")
	void shouldUpdateSummaryAndSelectIt() {
		// given
		Long linkId = 1L;
		Long summaryId = 100L;
		Member member = mock(Member.class);
		String content = "새롭게 수정된 요약 내용";
		Format format = Format.CONCISE;

		// Link Mocking (getLink -> getLinkForSummaryUpdate 로 변경됨)
		Link link = mock(Link.class);
		given(link.getId()).willReturn(linkId);
		given(linkService.getLinkForSummaryUpdate(linkId, member)).willReturn(link);

		// Summary Mocking
		Summary summary = mock(Summary.class);
		given(summary.getId()).willReturn(summaryId);
		given(summary.getContent()).willReturn(content);

		given(summaryService.createSummary(link, format, content)).willReturn(summary);
		doNothing().when(summaryService).selectSummary(linkId, summaryId);

		// when
		SummaryRes result = linkFacade.updateSummary(linkId, member, content, format);

		// then
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(summaryId);
		assertThat(result.content()).isEqualTo(content);

		verify(linkService, times(1)).getLinkForSummaryUpdate(linkId, member);
		verify(summaryService, times(1)).createSummary(link, format, content);
		verify(summaryService, times(1)).selectSummary(linkId, summaryId);
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

		Member member = Member
			.builder()
			.email("test@example.com")
			.password("password")
			.build();

		given(imageUploader.uploadFromUrl(originalImageUrl)).willReturn(storedImageUrl);

		Link savedLink = Link.builder()
			.member(member)
			.url(url)
			.title(title)
			.memo(memo)
			.imageUrl(storedImageUrl)
			.build();
		ReflectionTestUtils.setField(savedLink, "id", linkId);

		given(linkService.createLink(member, url, title, memo, storedImageUrl)).willReturn(savedLink);

		// when
		LinkRes result = linkFacade.createLink(member, url, title, memo, originalImageUrl);

		// then
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(linkId);

		verify(imageUploader, times(1)).uploadFromUrl(originalImageUrl);
		verify(linkService, times(1)).createLink(member, url, title, memo, storedImageUrl);
		verify(eventPublisher, times(1)).publishEvent(any(LinkCreatedEvent.class));
	}

	@Test
	@DisplayName("중복된 URL로 링크 생성 시 예외가 발생한다")
	void shouldThrowExceptionWhenDuplicateUrl() {
		// given
		Member member = Member
			.builder()
			.email("test@example.com")
			.password("password")
			.build();
		String url = "https://example.com";
		String originalImageUrl = "https://original.com/image.jpg";
		String storedImageUrl = "https://s3-bucket.com/stored-image.jpg";

		given(imageUploader.uploadFromUrl(originalImageUrl)).willReturn(storedImageUrl);
		given(linkService.createLink(member, url, "테스트 링크", null, storedImageUrl))
			.willThrow(new BusinessException(LinkErrorCode.DUPLICATE_URL));

		// when & then
		assertThatThrownBy(() -> linkFacade.createLink(
			member, url, "테스트 링크", null, originalImageUrl
		))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.DUPLICATE_URL);

		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("링크를 수정하고 LinkSyncEvent를 발행한다")
	void shouldUpdateLink() {
		// given
		Long linkId = 1L;
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();
		Link updatedLink = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("수정")
			.memo("메모수정")
			.build();
		ReflectionTestUtils.setField(updatedLink, "id", linkId);
		Summary mockSummary = mock(Summary.class);

		given(imageUploader.uploadFromUrl("https://example.com")).willReturn("https://example.com");
		given(linkService.updateLink(linkId, member, "수정", "메모수정", "https://example.com")).willReturn(updatedLink);
		given(summaryService.getSummaryOrElseNull(1L)).willReturn(mockSummary);

		// when
		LinkRes result = linkFacade.updateLink(linkId, member, "수정", "메모수정", "https://example.com");

		// then
		assertThat(result).isNotNull();
		assertThat(result.title()).isEqualTo("수정");
		assertThat(result.memo()).isEqualTo("메모수정");
		verify(linkService, times(1)).updateLink(linkId, member, "수정", "메모수정", "https://example.com");
		verify(summaryService).getSummaryOrElseNull(1L);
		verify(eventPublisher).publishEvent(any(LinkSyncEvent.class));
	}

	@Test
	@DisplayName("링크 제목을 수정하고 LinkSyncEvent를 발행한다")
	void shouldUpdateTitle() {
		// given
		Long linkId = 1L;
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();
		Link updatedLink = Link.builder()
			.member(member)
			.title("수정")
			.memo("원본메모")
			.build();
		ReflectionTestUtils.setField(updatedLink, "id", linkId);

		given(linkService.updateTitle(linkId, member, "수정")).willReturn(updatedLink);

		// when
		LinkRes result = linkFacade.updateTitle(linkId, member, "수정");

		// then
		assertThat(result.title()).isEqualTo("수정");
		verify(linkService, times(1)).updateTitle(linkId, member, "수정");
		verify(eventPublisher).publishEvent(any(LinkSyncEvent.class));
	}

	@Test
	@DisplayName("링크 메모를 수정하고 LinkSyncEvent를 발행한다")
	void shouldUpdateMemo() {
		// given
		Long linkId = 1L;
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();
		Link updatedLink = Link.builder()
			.member(member)
			.title("원본제목")
			.memo("수정")
			.build();
		ReflectionTestUtils.setField(updatedLink, "id", linkId);
		Summary mockSummary = mock(Summary.class);

		given(linkService.updateMemo(linkId, member, "수정")).willReturn(updatedLink);
		given(summaryService.getSummaryOrElseNull(1L)).willReturn(mockSummary);

		// when
		LinkRes result = linkFacade.updateMemo(linkId, member, "수정");

		// then
		assertThat(result.memo()).isEqualTo("수정");
		verify(linkService, times(1)).updateMemo(linkId, member, "수정");
		verify(summaryService).getSummaryOrElseNull(1L);
		verify(eventPublisher).publishEvent(any(LinkSyncEvent.class));
	}

	@Test
	@DisplayName("링크를 삭제하고 LinkSyncEvent(DELETE)를 발행한다")
	void shouldDeleteLink() {
		// given
		Long linkId = 1L;
		Member member = Member.builder().email("test@example.com").build();

		doNothing().when(linkService).deleteLink(linkId, member);

		// when
		linkFacade.deleteLink(linkId, member);

		// then
		verify(linkService, times(1)).deleteLink(linkId, member);
		verify(eventPublisher).publishEvent(any(LinkSyncEvent.class));
	}

	@Test
	@DisplayName("단일 링크 상세 조회를 수행한다")
	void shouldGetLinkDetail() {
		// given
		Long linkId = 1L;
		Member member = Member
			.builder()
			.email("test@example.com")
			.password("password")
			.build();
		Link link = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("제목")
			.build();
		ReflectionTestUtils.setField(link, "id", linkId);
		Summary summary = Summary.builder()
			.link(link)
			.content("요약내용")
			.build();

		LinkDto linkDto = new LinkDto(link, summary);

		given(linkService.getLinkWithSummary(linkId, member)).willReturn(linkDto);

		// when
		LinkDetailRes result = linkFacade.getLinkDetail(linkId, member);

		// then
		assertThat(result).isNotNull();
		assertThat(result.url()).isEqualTo("https://example.com");
		assertThat(result.summary().content()).isEqualTo("요약내용");
		verify(linkService, times(1)).getLinkWithSummary(linkId, member);
	}

	@Test
	@DisplayName("링크 카드 목록을 조회한다 (페이징 포함)")
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
		Member member = Member
			.builder()
			.email("test@example.com")
			.password("password")
			.build();
		String url = "https://example.com";

		given(linkService.findLinkIdByUrl(member, url)).willReturn(Optional.of(123L));

		// when
		LinkDuplicateCheckRes result = linkFacade.checkDuplicate(member, url);

		// then
		assertThat(result).isNotNull();
		verify(linkService, times(1)).findLinkIdByUrl(member, url);
	}

	@Test
	@DisplayName("사용자의 전체 링크 개수를 조회하여 응답 객체(DTO)로 반환한다")
	void shouldGetLinkTotalCount() {
		// given
		Member member = Member
			.builder()
			.email("test@example.com")
			.password("password")
			.build();
		given(linkService.getLinkTotalCount(member)).willReturn(15L);

		// when
		LinkTotalCountRes result = linkFacade.getLinkTotalCount(member);

		// then
		assertThat(result).isNotNull();
		assertThat(result.totalCount()).isEqualTo(15L);
		verify(linkService, times(1)).getLinkTotalCount(member);
	}
}
