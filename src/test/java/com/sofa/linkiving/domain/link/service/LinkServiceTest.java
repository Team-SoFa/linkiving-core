package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.entity.Link;
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
		given(linkCommandService.saveLink(any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
			.willReturn(link);

		// when
		LinkRes createdLink = linkService.createLink(
			member,
			"https://example.com",
			"테스트 링크",
			"메모",
			null,
			null,
			null,
			false
		);

		// then
		assertThat(createdLink).isNotNull();
		assertThat(createdLink.url()).isEqualTo("https://example.com");
		verify(linkQueryService, times(1)).existsByUrl(member, "https://example.com");
		verify(linkCommandService, times(1)).saveLink(any(), any(), any(), any(), any(), any(), any(), anyBoolean());
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
			null,
			null,
			null,
			false
		))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.DUPLICATE_URL);

		verify(linkQueryService, times(1)).existsByUrl(member, "https://example.com");
		verify(linkCommandService, never()).saveLink(any(), any(), any(), any(), any(), any(), any(), anyBoolean());
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
		given(linkCommandService.updateLink(any(), any(), any(), any(), any(), any())).willReturn(updatedLink);

		// when
		LinkRes result = linkService.updateLink(1L, member, "수정된 제목", null, null, null, null);

		// then
		assertThat(result).isNotNull();
		assertThat(result.title()).isEqualTo("수정된 제목");
		verify(linkQueryService, times(1)).findById(1L, member);
		verify(linkCommandService, times(1)).updateLink(any(), any(), any(), any(), any(), any());
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
		given(linkCommandService.updateLink(any(), eq("수정된 제목"), eq("원본 메모"), any(), any(), anyBoolean()))
			.willReturn(updatedLink);

		// when
		LinkRes result = linkService.updateTitle(1L, member, "수정된 제목");

		// then
		assertThat(result).isNotNull();
		assertThat(result.title()).isEqualTo("수정된 제목");
		assertThat(result.memo()).isEqualTo("원본 메모");
		verify(linkQueryService, times(1)).findById(1L, member);
		verify(linkCommandService, times(1)).updateLink(any(), eq("수정된 제목"), eq("원본 메모"), any(), any(), anyBoolean());
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
		given(linkCommandService.updateLink(any(), eq("원본 제목"), eq("수정된 메모"), any(), any(), anyBoolean()))
			.willReturn(updatedLink);

		// when
		LinkRes result = linkService.updateMemo(1L, member, "수정된 메모");

		// then
		assertThat(result).isNotNull();
		assertThat(result.title()).isEqualTo("원본 제목");
		assertThat(result.memo()).isEqualTo("수정된 메모");
		verify(linkQueryService, times(1)).findById(1L, member);
		verify(linkCommandService, times(1)).updateLink(any(), eq("원본 제목"), eq("수정된 메모"), any(), any(), anyBoolean());
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
		LinkRes result = linkService.getLink(1L, member);

		// then
		assertThat(result).isNotNull();
		assertThat(result.url()).isEqualTo("https://example.com");
		verify(linkQueryService, times(1)).findById(1L, member);
	}

	@Test
	@DisplayName("링크 목록을 조회할 수 있다")
	void shouldGetLinkList() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		Link link1 = Link.builder()
			.member(member)
			.url("https://example1.com")
			.title("링크 1")
			.build();

		Link link2 = Link.builder()
			.member(member)
			.url("https://example2.com")
			.title("링크 2")
			.build();

		Pageable pageable = PageRequest.of(0, 10);
		Page<Link> expectedPage = new PageImpl<>(List.of(link1, link2));

		given(linkQueryService.findAllByMember(member, pageable)).willReturn(expectedPage);

		// when
		Page<LinkRes> result = linkService.getLinkList(member, pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(2);
		verify(linkQueryService, times(1)).findAllByMember(member, pageable);
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
		LinkDuplicateCheckRes result = linkService.checkDuplicate(member, "https://example.com");

		// then
		assertThat(result.exists()).isTrue();
		assertThat(result.linkId()).isEqualTo(123L);
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
		LinkDuplicateCheckRes result = linkService.checkDuplicate(member, "https://example.com");

		// then
		assertThat(result.exists()).isFalse();
		assertThat(result.linkId()).isNull();
		verify(linkQueryService, times(1)).findIdByUrl(member, "https://example.com");
	}
}
