package com.sofa.linkiving.domain.link.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
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
	@DisplayName("ID로 링크와 선택된 요약 정보(LinkDto)를 함께 조회할 수 있다")
	void shouldFindByIdAndMemberWithSummary() {
		// given
		Link link = Link.builder()
			.member(testMember)
			.url("https://summary-test.com")
			.title("요약 테스트 링크")
			.build();
		entityManager.persist(link);

		Summary selectedSummary = Summary.builder()
			.link(link)
			.content("선택된 요약 내용")
			.format(Format.CONCISE)
			.selected(true)
			.build();
		entityManager.persist(selectedSummary);

		Summary otherSummary = Summary.builder()
			.link(link)
			.content("다른 요약")
			.format(Format.CONCISE)
			.selected(false)
			.build();
		entityManager.persist(otherSummary);

		entityManager.flush();
		entityManager.clear();

		// when
		Optional<LinkDto> result = linkRepository.findByIdAndMemberWithSummaryAndIsDeleteFalse(link.getId(),
			testMember);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().link().getUrl()).isEqualTo("https://summary-test.com");
		assertThat(result.get().summary()).isNotNull();
		assertThat(result.get().summary().getContent()).isEqualTo("선택된 요약 내용");
	}

	@Test
	@DisplayName("링크 목록을 요약 정보와 함께 커서 기반으로 조회할 수 있다")
	void shouldFindAllByMemberWithSummaryAndCursor() {
		// given
		for (int i = 1; i <= 3; i++) {
			Link link = Link.builder()
				.member(testMember)
				.url("https://paging-" + i + ".com")
				.title("링크 " + i)
				.build();
			entityManager.persist(link);

			// 짝수 번째 링크에만 요약 추가
			if (i % 2 == 0) {
				Summary summary = Summary.builder()
					.link(link)
					.content("요약 " + i)
					.format(Format.CONCISE)
					.selected(true)
					.build();
				entityManager.persist(summary);
			}
		}
		entityManager.flush();
		entityManager.clear();

		// when 1
		List<LinkDto> page1 = linkRepository.findAllByMemberWithSummaryAndCursorAndIsDeleteFalse(
			testMember, null, PageRequest.of(0, 2));

		// then 1
		assertThat(page1).hasSize(2);
		assertThat(page1.get(0).link().getTitle()).isEqualTo("링크 3");
		assertThat(page1.get(0).summary()).isNull(); // 요약 없음

		assertThat(page1.get(1).link().getTitle()).isEqualTo("링크 2");
		assertThat(page1.get(1).summary()).isNotNull(); // 요약 있음
		assertThat(page1.get(1).summary().getContent()).isEqualTo("요약 2");

		Long lastId = page1.get(1).link().getId();

		// when 2 - 다음 내용
		List<LinkDto> page2 = linkRepository.findAllByMemberWithSummaryAndCursorAndIsDeleteFalse(
			testMember, lastId, PageRequest.of(0, 2));

		// then 2
		assertThat(page2).hasSize(1);
		assertThat(page2.get(0).link().getTitle()).isEqualTo("링크 1");
	}

	@Test
	@DisplayName("조건에 맞는 링크와 선택된 요약을 조회한다")
	void shouldReturnLinksWithSelectedSummary() {
		// given
		Member otherMember = Member.builder()
			.email("other@test.com")
			.password("password")
			.build();
		entityManager.persist(otherMember);

		Link link1 = linkRepository.save(Link.builder()
			.member(testMember)
			.title("link1")
			.url("http://url1.com")
			.build());
		Summary summary1 = Summary.builder()
			.link(link1)
			.content("요약1")
			.selected(true)
			.build();
		entityManager.persist(summary1);

		Link link2 = linkRepository.save(Link.builder()
			.member(testMember)
			.title("link2")
			.url("http://url2.com")
			.build());
		Summary summary2 = Summary.builder()
			.link(link2)
			.content("요약2")
			.selected(false)
			.build();
		entityManager.persist(summary2);

		Link link3 = linkRepository.save(Link.builder()
			.member(testMember)
			.title("link3")
			.url("http://url3.com")
			.build());

		Link link4 = linkRepository.save(Link.builder()
			.member(otherMember)
			.title("link4")
			.url("http://url4.com")
			.build());

		List<Long> linkIds = List.of(link1.getId(), link2.getId(), link3.getId(), link4.getId());

		// when
		List<LinkDto> result = linkRepository.findAllByMemberAndIdInWithSummaryAndIsDeleteFalse(linkIds, testMember);

		// then
		assertThat(result).hasSize(3);

		List<Long> resultIds = result.stream().map(dto -> dto.link().getId()).toList();
		assertThat(resultIds).containsExactlyInAnyOrder(link1.getId(), link2.getId(), link3.getId());
	}

	@Test
	@DisplayName("요청한 ID 목록에 해당하는 링크가 없으면 빈 리스트를 반환한다")
	void shouldReturnEmptyList_WhenNoMatch() {
		// given
		List<Long> nonExistentIds = List.of(999L, 1000L);

		// when
		List<LinkDto> result = linkRepository.findAllByMemberAndIdInWithSummaryAndIsDeleteFalse(nonExistentIds,
			testMember);

		// then
		assertThat(result).isEmpty();
	}
}
