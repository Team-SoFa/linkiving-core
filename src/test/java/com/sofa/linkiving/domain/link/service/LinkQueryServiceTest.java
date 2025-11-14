package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkQueryService 기본 조회 테스트")
class LinkQueryServiceTest {

	@Mock
	private LinkRepository linkRepository;

	@InjectMocks
	private LinkQueryService linkQueryService;

	private Member testMember;
	private Link testLink;
	private Pageable pageable;

	@BeforeEach
	void setUp() {
		testMember = Member.builder()
			.email("test@example.com")
			.password("password123")
			.build();

		testLink = Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("테스트 링크")
			.memo("테스트 메모")
			.imageUrl("https://example.com/image.jpg")
			.metadataJson("{\"key\":\"value\"}")
			.tags("[\"tag1\",\"tag2\"]")
			.isImportant(false)
			.build();

		pageable = PageRequest.of(0, 10);
	}

	@Test
	@DisplayName("ID로 링크를 조회할 수 있다")
	void shouldFindById() {
		// given
		when(linkRepository.findByIdAndMember(1L, testMember))
			.thenReturn(Optional.of(testLink));

		// when
		Link foundLink = linkQueryService.findById(1L, testMember);

		// then
		assertThat(foundLink).isNotNull();
		assertThat(foundLink.getUrl()).isEqualTo("https://example.com");
		assertThat(foundLink.getTitle()).isEqualTo("테스트 링크");
	}

	@Test
	@DisplayName("존재하지 않는 링크 조회 시 예외가 발생한다")
	void shouldThrowExceptionWhenLinkNotFound() {
		// given
		when(linkRepository.findByIdAndMember(999L, testMember))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> linkQueryService.findById(999L, testMember))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.LINK_NOT_FOUND);
	}

	@Test
	@DisplayName("멤버의 전체 링크를 조회할 수 있다")
	void shouldFindAllByMember() {
		// given
		List<Link> linkList = List.of(testLink, testLink);
		Page<Link> linkPage = new PageImpl<>(linkList, pageable, 2);
		when(linkRepository.findByMemberAndIsDeleteFalse(testMember, pageable))
			.thenReturn(linkPage);

		// when
		Page<Link> result = linkQueryService.findAllByMember(testMember, pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).hasSize(2);
	}

	@Test
	@DisplayName("URL 중복을 확인할 수 있다")
	void shouldCheckUrlExists() {
		// given
		when(linkRepository.existsByMemberAndUrlAndIsDeleteFalse(testMember, "https://example.com"))
			.thenReturn(true);

		// when
		boolean exists = linkQueryService.existsByUrl(testMember, "https://example.com");

		// then
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("삭제된 링크는 조회되지 않는다")
	void shouldNotReturnDeletedLink() {
		// given
		testLink.markDeleted();
		when(linkRepository.findByIdAndMember(1L, testMember))
			.thenReturn(Optional.of(testLink));

		// when & then
		assertThatThrownBy(() -> linkQueryService.findById(1L, testMember))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.LINK_NOT_FOUND);
	}

	@Test
	@DisplayName("페이징 - 여러 페이지에 걸친 링크 조회가 가능하다")
	void shouldFindLinksByMultiplePages() {
		// given
		Pageable firstPage = PageRequest.of(0, 5);
		Pageable secondPage = PageRequest.of(1, 5);

		List<Link> firstPageLinks = List.of(testLink, testLink, testLink, testLink, testLink);
		List<Link> secondPageLinks = List.of(testLink, testLink, testLink);

		Page<Link> firstPageResult = new PageImpl<>(firstPageLinks, firstPage, 8);
		Page<Link> secondPageResult = new PageImpl<>(secondPageLinks, secondPage, 8);

		when(linkRepository.findByMemberAndIsDeleteFalse(testMember, firstPage))
			.thenReturn(firstPageResult);
		when(linkRepository.findByMemberAndIsDeleteFalse(testMember, secondPage))
			.thenReturn(secondPageResult);

		// when
		Page<Link> firstResult = linkQueryService.findAllByMember(testMember, firstPage);
		Page<Link> secondResult = linkQueryService.findAllByMember(testMember, secondPage);

		// then
		assertThat(firstResult.getContent()).hasSize(5);
		assertThat(firstResult.getTotalElements()).isEqualTo(8);
		assertThat(firstResult.getTotalPages()).isEqualTo(2);
		assertThat(firstResult.hasNext()).isTrue();
		assertThat(firstResult.isFirst()).isTrue();

		assertThat(secondResult.getContent()).hasSize(3);
		assertThat(secondResult.getTotalElements()).isEqualTo(8);
		assertThat(secondResult.getTotalPages()).isEqualTo(2);
		assertThat(secondResult.hasNext()).isFalse();
		assertThat(secondResult.isLast()).isTrue();
	}

	@Test
	@DisplayName("페이징 - 빈 결과를 조회할 수 있다")
	void shouldReturnEmptyPage() {
		// given
		Page<Link> emptyPage = new PageImpl<>(List.of(), pageable, 0);
		when(linkRepository.findByMemberAndIsDeleteFalse(testMember, pageable))
			.thenReturn(emptyPage);

		// when
		Page<Link> result = linkQueryService.findAllByMember(testMember, pageable);

		// then
		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isEqualTo(0);
		assertThat(result.getTotalPages()).isEqualTo(0);
		assertThat(result.hasNext()).isFalse();
		assertThat(result.hasPrevious()).isFalse();
	}

	@Test
	@DisplayName("페이징 - 페이지 크기가 다를 때도 정상 동작한다")
	void shouldWorkWithDifferentPageSizes() {
		// given
		Pageable smallPage = PageRequest.of(0, 3);
		Pageable largePage = PageRequest.of(0, 20);

		List<Link> links = List.of(testLink, testLink, testLink, testLink, testLink);
		Page<Link> smallPageResult = new PageImpl<>(links.subList(0, 3), smallPage, 5);
		Page<Link> largePageResult = new PageImpl<>(links, largePage, 5);

		when(linkRepository.findByMemberAndIsDeleteFalse(testMember, smallPage))
			.thenReturn(smallPageResult);
		when(linkRepository.findByMemberAndIsDeleteFalse(testMember, largePage))
			.thenReturn(largePageResult);

		// when
		Page<Link> smallResult = linkQueryService.findAllByMember(testMember, smallPage);
		Page<Link> largeResult = linkQueryService.findAllByMember(testMember, largePage);

		// then
		assertThat(smallResult.getContent()).hasSize(3);
		assertThat(smallResult.getTotalPages()).isEqualTo(2);
		assertThat(smallResult.hasNext()).isTrue();

		assertThat(largeResult.getContent()).hasSize(5);
		assertThat(largeResult.getTotalPages()).isEqualTo(1);
		assertThat(largeResult.hasNext()).isFalse();
	}

	@Test
	@DisplayName("페이징 - 삭제된 링크는 페이징 결과에 포함되지 않는다")
	void shouldNotIncludeDeletedLinksInPagedResults() {
		// given
		List<Link> activeLinksOnly = List.of(testLink, testLink);
		Page<Link> linkPage = new PageImpl<>(activeLinksOnly, pageable, 2);
		when(linkRepository.findByMemberAndIsDeleteFalse(testMember, pageable))
			.thenReturn(linkPage);

		// when
		Page<Link> result = linkQueryService.findAllByMember(testMember, pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).hasSize(2);
		verify(linkRepository).findByMemberAndIsDeleteFalse(testMember, pageable);
	}
}
