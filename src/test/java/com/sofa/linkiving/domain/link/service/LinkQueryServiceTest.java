package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.dto.internal.LinksDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkQueryService 단위 테스트")
class LinkQueryServiceTest {

	@InjectMocks
	private LinkQueryService linkQueryService;

	@Mock
	private LinkRepository linkRepository;

	@Test
	@DisplayName("ID로 링크를 조회할 수 있다")
	void shouldFindById() {
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

		given(linkRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(link));

		// when
		Link foundLink = linkQueryService.findById(1L, member);

		// then
		assertThat(foundLink).isNotNull();
		assertThat(foundLink.getUrl()).isEqualTo("https://example.com");
		verify(linkRepository, times(1)).findByIdAndMember(1L, member);
	}

	@Test
	@DisplayName("존재하지 않는 링크 조회 시 예외가 발생한다")
	void shouldThrowExceptionWhenLinkNotFound() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		given(linkRepository.findByIdAndMember(999L, member)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> linkQueryService.findById(999L, member))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.LINK_NOT_FOUND);
	}

	@Test
	@DisplayName("URL 중복 여부를 확인할 수 있다")
	void shouldCheckUrlExists() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		given(linkRepository.existsByMemberAndUrlAndIsDeleteFalse(member, "https://example.com"))
			.willReturn(true);

		// when
		boolean exists = linkQueryService.existsByUrl(member, "https://example.com");

		// then
		assertThat(exists).isTrue();
		verify(linkRepository, times(1))
			.existsByMemberAndUrlAndIsDeleteFalse(member, "https://example.com");
	}

	@Test
	@DisplayName("커서 기반 목록 조회 시 요청 개수보다 데이터가 많으면 hasNext=true를 반환함")
	void shouldFindAllByMemberWithSummaryAndCursor_HasNextTrue() {
		// given
		Member member = mock(Member.class);
		Long lastId = 100L;
		int size = 10;

		// Repository가 size + 1 (11개) 데이터를 반환한다고 가정
		List<LinkDto> dtos = new ArrayList<>();
		for (int i = 0; i < size + 1; i++) {
			dtos.add(mock(LinkDto.class));
		}

		// Pageable 검증 (size + 1 로 요청했는지)
		given(linkRepository.findAllByMemberWithSummaryAndCursorAndIsDeleteFalse(
			eq(member), eq(lastId), any(Pageable.class)))
			.willReturn(dtos);

		// when
		LinksDto result = linkQueryService.findAllByMemberWithSummaryAndCursor(member, lastId, size);

		// then
		assertThat(result.hasNext()).isTrue();
		assertThat(result.linkDtos()).hasSize(size); // 11개 -> 10개로 잘림
	}

	@Test
	@DisplayName("커서 기반 목록 조회 시 데이터가 요청 개수 이하이면 hasNext=false를 반환함")
	void shouldFindAllByMemberWithSummaryAndCursor_HasNextFalse() {
		// given
		Member member = mock(Member.class);
		Long lastId = 100L;
		int size = 10;

		// Repository가 딱 size (10개) 데이터를 반환한다고 가정
		List<LinkDto> dtos = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			dtos.add(mock(LinkDto.class));
		}

		given(linkRepository.findAllByMemberWithSummaryAndCursorAndIsDeleteFalse(
			eq(member), eq(lastId), any(Pageable.class)))
			.willReturn(dtos);

		// when
		LinksDto result = linkQueryService.findAllByMemberWithSummaryAndCursor(member, lastId, size);

		// then
		assertThat(result.hasNext()).isFalse();
		assertThat(result.linkDtos()).hasSize(size); // 그대로 10개
	}

	@Test
	@DisplayName("ID로 링크와 요약 정보(LinkDto)를 조회할 수 있다")
	void shouldFindByIdWithSummary() {
		// given
		Member member = mock(Member.class);
		Link link = mock(Link.class);
		Summary summary = mock(Summary.class);

		LinkDto expectedDto = new LinkDto(link, summary);

		given(linkRepository.findByIdAndMemberWithSummaryAndIsDeleteFalse(1L, member))
			.willReturn(Optional.of(expectedDto));

		// when
		LinkDto result = linkQueryService.findByIdWithSummary(1L, member);

		// then
		assertThat(result).isEqualTo(expectedDto);
		verify(linkRepository).findByIdAndMemberWithSummaryAndIsDeleteFalse(1L, member);
	}

	@Test
	@DisplayName("요약 포함 조회 시 존재하지 않거나 삭제된 링크면 예외가 발생한다")
	void shouldThrowExceptionWhenLinkNotFoundInFindByIdWithSummary() {
		// given
		Member member = mock(Member.class);
		Long linkId = 999L;

		given(linkRepository.findByIdAndMemberWithSummaryAndIsDeleteFalse(linkId, member))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> linkQueryService.findByIdWithSummary(linkId, member))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.LINK_NOT_FOUND);
	}
}
