package com.sofa.linkiving.domain.link.repository;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.member.entity.Member;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("LinkRepository 기본 CRUD 테스트")
class LinkRepositoryTest {

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private EntityManager entityManager;

	private Member testMember;

	@BeforeEach
	void setUp() {
		testMember = Member.builder()
			.email("test@example.com")
			.password("password123")
			.build();
		entityManager.persist(testMember);
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	@DisplayName("링크를 저장할 수 있다")
	void shouldSaveLink() {
		// given
		Link link = Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("테스트 링크")
			.memo("테스트 메모")
			.imageUrl("https://example.com/image.jpg")
			.metadataJson("{\"key\":\"value\"}")
			.tags("[\"tag1\",\"tag2\"]")
			.isImportant(false)
			.build();

		// when
		Link savedLink = linkRepository.save(link);

		// then
		assertThat(savedLink).isNotNull();
		assertThat(savedLink.getId()).isNotNull();
		assertThat(savedLink.getUrl()).isEqualTo("https://example.com");
		assertThat(savedLink.getTitle()).isEqualTo("테스트 링크");
	}

	@Test
	@DisplayName("ID와 Member로 링크를 조회할 수 있다")
	void shouldFindByIdAndMember() {
		// given
		Link link = Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("테스트 링크")
			.build();
		Link savedLink = linkRepository.save(link);

		// when
		Link foundLink = linkRepository.findByIdAndMember(savedLink.getId(), testMember)
			.orElseThrow();

		// then
		assertThat(foundLink.getId()).isEqualTo(savedLink.getId());
		assertThat(foundLink.getUrl()).isEqualTo("https://example.com");
	}

	@Test
	@DisplayName("멤버의 링크 목록을 페이징 조회할 수 있다")
	void shouldFindByMemberWithPaging() {
		// given
		Link link1 = Link.builder()
			.member(testMember)
			.url("https://example1.com")
			.title("링크 1")
			.build();
		Link link2 = Link.builder()
			.member(testMember)
			.url("https://example2.com")
			.title("링크 2")
			.build();
		linkRepository.save(link1);
		linkRepository.save(link2);

		// when
		Page<Link> links = linkRepository.findByMemberAndIsDeleteFalse(
			testMember, PageRequest.of(0, 10));

		// then
		assertThat(links.getTotalElements()).isEqualTo(2);
		assertThat(links.getContent()).hasSize(2);
	}

	@Test
	@DisplayName("URL 중복을 체크할 수 있다")
	void shouldCheckUrlDuplication() {
		// given
		Link link = Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("테스트 링크")
			.build();
		linkRepository.save(link);

		// when
		boolean exists = linkRepository.existsByMemberAndUrlAndIsDeleteFalse(
			testMember, "https://example.com");
		boolean notExists = linkRepository.existsByMemberAndUrlAndIsDeleteFalse(
			testMember, "https://notexist.com");

		// then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
	}

	@Test
	@DisplayName("삭제된 링크는 조회되지 않는다")
	void shouldNotFindDeletedLink() {
		// given
		Link link = Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("테스트 링크")
			.build();
		Link savedLink = linkRepository.save(link);

		savedLink.markDeleted();
		linkRepository.save(savedLink);

		// when
		Page<Link> links = linkRepository.findByMemberAndIsDeleteFalse(
			testMember, PageRequest.of(0, 10));

		// then
		assertThat(links.getTotalElements()).isEqualTo(0);
	}
}
