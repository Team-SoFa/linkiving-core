package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkCommandService 기본 CRUD 테스트")
class LinkCommandServiceTest {

	@Mock
	private LinkRepository linkRepository;

	@Mock
	private LinkQueryService linkQueryService;

	@InjectMocks
	private LinkCommandService linkCommandService;

	private Member testMember;
	private Link testLink;

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
	}

	@Test
	@DisplayName("새로운 링크를 생성할 수 있다")
	void shouldCreateLink() {
		// given
		when(linkRepository.existsByMemberAndUrlAndIsDeleteFalse(any(), any()))
			.thenReturn(false);
		when(linkRepository.save(any(Link.class)))
			.thenReturn(testLink);

		// when
		Link createdLink = linkCommandService.createLink(
			testMember,
			"https://example.com",
			"테스트 링크",
			"테스트 메모",
			"https://example.com/image.jpg",
			"{\"key\":\"value\"}",
			"[\"tag1\",\"tag2\"]",
			false
		);

		// then
		assertThat(createdLink).isNotNull();
		assertThat(createdLink.getUrl()).isEqualTo("https://example.com");
		assertThat(createdLink.getTitle()).isEqualTo("테스트 링크");
		verify(linkRepository).save(any(Link.class));
	}

	@Test
	@DisplayName("중복된 URL로 링크를 생성할 수 없다")
	void shouldNotCreateDuplicateUrl() {
		// given
		when(linkRepository.existsByMemberAndUrlAndIsDeleteFalse(any(), any()))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> linkCommandService.createLink(
			testMember,
			"https://example.com",
			"테스트 링크",
			null, null, null, null, false
		))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.DUPLICATE_URL);
	}

	@Test
	@DisplayName("링크를 삭제할 수 있다")
	void shouldDeleteLink() {
		// given
		when(linkQueryService.findById(1L, testMember))
			.thenReturn(testLink);

		// when
		linkCommandService.deleteLink(1L, testMember);

		// then
		assertThat(testLink.isDeleted()).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 링크에 접근하면 예외가 발생한다")
	void shouldThrowExceptionWhenLinkNotFound() {
		// given
		when(linkQueryService.findById(999L, testMember))
			.thenThrow(new BusinessException(LinkErrorCode.LINK_NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> linkCommandService.deleteLink(999L, testMember))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.LINK_NOT_FOUND);
	}
}
