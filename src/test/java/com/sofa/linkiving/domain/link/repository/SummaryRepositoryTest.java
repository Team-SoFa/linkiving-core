package com.sofa.linkiving.domain.link.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.member.entity.Member;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SummaryRepository 단위 테스트")
public class SummaryRepositoryTest {

	@Autowired
	private SummaryRepository summaryRepository;

	@Autowired
	private TestEntityManager em;

	@Test
	@DisplayName("주어진 링크들에 속하고 selected가 true인 요약만 조회함")
	void shouldFindAllByLinkInAndSelectedTrue() {
		// given
		Member member = Member.builder()
			.email("test@test.com")
			.password("pw")
			.build();
		em.persist(member);

		Link link1 = Link.builder()
			.member(member)
			.url("url1")
			.title("t1")
			.build();
		Link link2 = Link.builder()
			.member(member)
			.url("url2")
			.title("t2")
			.build();
		Link link3 = Link.builder()
			.member(member)
			.url("url3")
			.title("t3")
			.build();

		em.persist(link1);
		em.persist(link2);
		em.persist(link3);

		Summary summary1 = Summary.builder()
			.link(link1)
			.content("s1")
			.selected(true)
			.build();
		Summary summary2 = Summary.builder()
			.link(link2)
			.content("s2")
			.selected(false)
			.build();
		Summary summary3 = Summary
			.builder()
			.link(link3)
			.content("s3")
			.selected(true)
			.build();

		em.persist(summary1);
		em.persist(summary2);
		em.persist(summary3);

		em.flush();
		em.clear();

		// when
		List<Summary> result = summaryRepository.findAllByLinkInAndSelectedTrue(List.of(link1, link2));

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getContent()).isEqualTo("s1");
		assertThat(result.get(0).getLink().getId()).isEqualTo(link1.getId());
	}

	@Test
	@DisplayName("링크 리스트가 비어있으면 빈 결과를 반환함")
	void shouldReturnEmptyWhenLinkListIsEmpty() {
		// given
		Member member = Member.builder()
			.email("test@test.com")
			.password("pw")
			.build();
		em.persist(member);

		Link link = Link.builder()
			.member(member)
			.url("url1")
			.title("t1")
			.build();
		em.persist(link);

		Summary summary = Summary.builder()
			.link(link)
			.content("s1")
			.selected(true)
			.build();
		em.persist(summary);

		// when
		List<Summary> result = summaryRepository.findAllByLinkInAndSelectedTrue(List.of());

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("clearSelectedByLinkId 및 selectByIdAndLinkId 실행 시 selected 요약이 단 1개만 존재한다")
	void shouldEnsureOnlyOneSummaryIsSelected() {
		// given
		Member member = Member.builder()
			.email("test@test.com")
			.password("pw")
			.build();
		em.persist(member);
		Link link = Link.builder()
			.member(member)
			.url("http://test.com")
			.title("t1")
			.build();
		em.persist(link);

		// 1. 과거 요약 1 (기존 선택됨)
		Summary oldSummary1 = Summary.builder()
			.link(link)
			.content("과거 요약 1")
			.format(Format.CONCISE)
			.selected(true)
			.build();

		// 2. 과거 요약 2 (선택 안됨)
		Summary oldSummary2 = Summary.builder()
			.link(link)
			.content("과거 요약 2")
			.format(Format.DETAILED)
			.selected(false)
			.build();

		// 3. 방금 새로 수정한 요약 (아직 선택 안됨)
		Summary newSummary = Summary.builder()
			.link(link)
			.content("새로 수정한 요약")
			.format(Format.DETAILED).selected(false)
			.build();

		em.persist(oldSummary1);
		em.persist(oldSummary2);
		em.persist(newSummary);

		// when - Service 계층에서 수행하는 두 가지 쿼리를 순서대로 실행

		summaryRepository.clearSelectedByLinkId(link.getId());
		summaryRepository.selectByIdAndLinkId(newSummary.getId(), link.getId());

		// then
		Optional<Summary> selectedSummary = summaryRepository.findByLinkIdAndSelectedTrue(link.getId());
		assertThat(selectedSummary).isPresent();
		assertThat(selectedSummary.get().getId()).isEqualTo(newSummary.getId());

		long selectedCount = summaryRepository.findAll().stream()
			.filter(s -> s.getLink().getId().equals(link.getId()))
			.filter(Summary::isSelected)
			.count();

		assertThat(selectedCount).isEqualTo(1L);
	}
}
