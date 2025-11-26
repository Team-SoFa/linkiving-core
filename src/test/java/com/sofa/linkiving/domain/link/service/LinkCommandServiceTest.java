package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkCommandService 단위 테스트")
class LinkCommandServiceTest {

	@InjectMocks
	private LinkCommandService linkCommandService;

	@Mock
	private LinkRepository linkRepository;

	@Test
	@DisplayName("링크를 저장할 수 있다")
	void shouldSaveLink() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("테스트 링크")
			.memo("메모")
			.imageUrl("https://example.com/image.jpg")
			.metadataJson("{}")
			.tags("tag1,tag2")
			.isImportant(false)
			.build();

		given(linkRepository.save(any(Link.class))).willReturn(link);

		// when
		Link savedLink = linkCommandService.saveLink(
			member,
			"https://example.com",
			"테스트 링크",
			"메모",
			"https://example.com/image.jpg",
			"{}",
			"tag1,tag2",
			false
		);

		// then
		assertThat(savedLink).isNotNull();
		assertThat(savedLink.getUrl()).isEqualTo("https://example.com");
		verify(linkRepository, times(1)).save(any(Link.class));
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
			.memo("원본 메모")
			.imageUrl("https://example.com/image.jpg")
			.metadataJson("{}")
			.tags("tag1")
			.isImportant(false)
			.build();

		// when
		Link result = linkCommandService.updateLink(
			originalLink,
			"수정된 제목",
			"수정된 메모",
			"{}",
			"tag1,tag2",
			true
		);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo("수정된 제목");
		assertThat(result.getMemo()).isEqualTo("수정된 메모");
		assertThat(result.getMetadataJson()).isEqualTo("{}");
		assertThat(result.getTags()).isEqualTo("tag1,tag2");
		assertThat(result.isImportant()).isTrue();
		verify(linkRepository, never()).save(any(Link.class));
	}

	@Test
	@DisplayName("링크를 소프트 삭제할 수 있다")
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

		// when
		linkCommandService.deleteLink(link);

		// then
		assertThat(link.isDeleted()).isTrue();
	}
}
