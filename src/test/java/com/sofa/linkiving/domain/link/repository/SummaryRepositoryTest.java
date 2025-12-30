package com.sofa.linkiving.domain.link.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
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
			.select(true)
			.build();
		Summary summary2 = Summary.builder()
			.link(link2)
			.content("s2")
			.select(false)
			.build();
		Summary summary3 = Summary
			.builder()
			.link(link3)
			.content("s3")
			.select(true)
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
			.select(true)
			.build();
		em.persist(summary);

		// when
		List<Summary> result = summaryRepository.findAllByLinkInAndSelectedTrue(List.of());

		// then
		assertThat(result).isEmpty();
	}
}
