package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.dto.internal.LinksDto;
import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkService 단위 테스트")
class LinkServiceTest {

	@InjectMocks
	private LinkService linkService;

	@Mock
	private LinkCommandService linkCommandService;

	@Mock
	private LinkQueryService linkQueryService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Test
	@DisplayName("링크를 생성할 수 있다")
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

		given(linkQueryService.existsByUrl(member, "https://example.com")).willReturn(false);
		given(linkCommandService.saveLink(any(), any(), any(), any(), any()))
			.willReturn(link);

		// when
		Link save = linkService.createLink(
			member,
			"https://example.com",
			"테스트 링크",
			"메모",
			null
		);

		// then
		assertThat(save).isNotNull();
		assertThat(save.getUrl()).isEqualTo("https://example.com");
		verify(linkQueryService, times(1)).existsByUrl(member, "https://example.com");
		verify(linkCommandService, times(1)).saveLink(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("중복된 URL로 링크 생성 시 예외가 발생한다")
	void shouldThrowExceptionWhenDuplicateUrl() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		given(linkQueryService.existsByUrl(member, "https://example.com")).willReturn(true);

		// when & then
		assertThatThrownBy(() -> linkService.createLink(
			member,
			"https://example.com",
			"테스트 링크",
			null,
			null
		))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.DUPLICATE_URL);

		verify(linkQueryService, times(1)).existsByUrl(member, "https://example.com");
		verify(linkCommandService, never()).saveLink(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("링크를 수정할 수 있다")
	void shouldUpdateLink() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		Link originalLink = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("원본 제목")
			.build();

		Link updatedLink = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("수정된 제목")
			.build();

		given(linkQueryService.findById(1L, member)).willReturn(originalLink);
		given(linkCommandService.updateLink(any(), any(), any())).willReturn(updatedLink);

		// when
		Link result = linkService.updateLink(1L, member, "수정된 제목", null);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo("수정된 제목");
		verify(linkQueryService, times(1)).findById(1L, member);
		verify(linkCommandService, times(1)).updateLink(any(), any(), any());
	}

	@Test
	@DisplayName("링크 제목만 수정할 수 있다")
	void shouldUpdateTitle() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		Link originalLink = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("원본 제목")
			.memo("원본 메모")
			.build();

		Link updatedLink = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("수정된 제목")
			.memo("원본 메모")
			.build();

		given(linkQueryService.findById(1L, member)).willReturn(originalLink);
		given(linkCommandService.updateLink(any(), eq("수정된 제목"), eq("원본 메모")))
			.willReturn(updatedLink);

		// when
		Link result = linkService.updateTitle(1L, member, "수정된 제목");

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo("수정된 제목");
		assertThat(result.getMemo()).isEqualTo("원본 메모");
		verify(linkQueryService, times(1)).findById(1L, member);
		verify(linkCommandService, times(1)).updateLink(any(), eq("수정된 제목"), eq("원본 메모"));
	}

	@Test
	@DisplayName("링크 메모만 수정할 수 있다")
	void shouldUpdateMemo() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		Link originalLink = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("원본 제목")
			.memo("원본 메모")
			.build();

		Link updatedLink = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("원본 제목")
			.memo("수정된 메모")
			.build();

		given(linkQueryService.findById(1L, member)).willReturn(originalLink);
		given(linkCommandService.updateLink(any(), eq("원본 제목"), eq("수정된 메모")))
			.willReturn(updatedLink);

		// when
		Link result = linkService.updateMemo(1L, member, "수정된 메모");

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo("원본 제목");
		assertThat(result.getMemo()).isEqualTo("수정된 메모");
		verify(linkQueryService, times(1)).findById(1L, member);
		verify(linkCommandService, times(1)).updateLink(any(), eq("원본 제목"), eq("수정된 메모"));
	}

	@Test
	@DisplayName("링크를 삭제할 수 있다")
	void shouldDeleteLink() {
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

		given(linkQueryService.findById(1L, member)).willReturn(link);

		// when
		linkService.deleteLink(1L, member);

		// then
		verify(linkQueryService, times(1)).findById(1L, member);
		verify(linkCommandService, times(1)).deleteLink(link);
	}

	@Test
	@DisplayName("링크를 조회할 수 있다")
	void shouldGetLink() {
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

		given(linkQueryService.findById(1L, member)).willReturn(link);

		// when
		Link result = linkService.getLink(1L, member);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getUrl()).isEqualTo("https://example.com");
		verify(linkQueryService, times(1)).findById(1L, member);
	}

	@Test
	@DisplayName("링크 (요약 포함) 단건 조회 시 LinkDto를 반환함")
	void shouldGetLinkWithSummary() {
		// given
		Member member = mock(Member.class);

		// Link Mock
		Link link = mock(Link.class);

		Summary summary = mock(Summary.class);

		LinkDto linkDto = new LinkDto(link, summary);

		given(linkQueryService.findByIdWithSummary(1L, member)).willReturn(linkDto);

		// when
		LinkDto result = linkService.getLinkWithSummary(1L, member);

		// then
		assertThat(result).isNotNull();
		assertThat(result.link()).isEqualTo(link);
		assertThat(result.summary()).isEqualTo(summary);

		verify(linkQueryService).findByIdWithSummary(1L, member);
	}

	@Test
	@DisplayName("링크 목록 조회 시 커서 기반 페이징된 LinksRes를 반환함")
	void shouldGetLinksWithSummary() {
		// given
		Member member = mock(Member.class);
		Long lastId = 10L;
		int size = 5;

		// Link & LinkDto Mock
		Link link = mock(Link.class);

		LinkDto linkDto = new LinkDto(link, null); // 요약 없음 가정
		List<LinkDto> dtos = List.of(linkDto);

		LinksDto linksDto = new LinksDto(dtos, true); // 다음 페이지 있음

		given(linkQueryService.findAllByMemberWithSummaryAndCursor(member, lastId, size))
			.willReturn(linksDto);

		// when
		LinksDto result = linkService.getLinksWithSummary(member, lastId, size);

		// then
		assertThat(result).isNotNull();
		assertThat(result.linkDtos()).hasSize(1);
		assertThat(result.hasNext()).isTrue();

		verify(linkQueryService).findAllByMemberWithSummaryAndCursor(member, lastId, size);
	}

	@Test
	@DisplayName("URL 중복 체크 - 중복된 링크 존재")
	void shouldCheckDuplicate() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		given(linkQueryService.findIdByUrl(member, "https://example.com")).willReturn(java.util.Optional.of(123L));

		// when
		Optional<Long> result = linkService.findLinkIdByUrl(member, "https://example.com");

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(123L);
		verify(linkQueryService, times(1)).findIdByUrl(member, "https://example.com");
	}

	@Test
	@DisplayName("URL 중복 체크 - 중복된 링크 없음")
	void shouldCheckDuplicateNotExists() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		given(linkQueryService.findIdByUrl(member, "https://example.com")).willReturn(java.util.Optional.empty());

		// when
		Optional<Long> result = linkService.findLinkIdByUrl(member, "https://example.com");

		// then
		assertThat(result).isEmpty();
		verify(linkQueryService, times(1)).findIdByUrl(member, "https://example.com");
	}
}
