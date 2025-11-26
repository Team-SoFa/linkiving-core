package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

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
	@DisplayName("멤버의 모든 링크를 페이징 조회할 수 있다")
	void shouldFindAllByMember() {
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

		given(linkRepository.findByMemberAndIsDeleteFalse(member, pageable)).willReturn(expectedPage);

		// when
		Page<Link> result = linkQueryService.findAllByMember(member, pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(2);
		verify(linkRepository, times(1)).findByMemberAndIsDeleteFalse(member, pageable);
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
}
