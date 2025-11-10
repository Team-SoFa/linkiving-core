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
}
