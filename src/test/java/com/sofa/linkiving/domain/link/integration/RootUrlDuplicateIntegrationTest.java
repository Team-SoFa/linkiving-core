package com.sofa.linkiving.domain.link.integration;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.service.LinkCommandService;
import com.sofa.linkiving.domain.link.service.LinkQueryService;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.util.UrlNormalizer;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@Import({LinkService.class, LinkQueryService.class, LinkCommandService.class, UrlNormalizer.class})
class RootUrlDuplicateIntegrationTest {

	@Autowired
	private LinkService linkService;

	@Autowired
	private EntityManager entityManager;

	private Member member;

	@BeforeEach
	void setUp() {
		member = Member.builder().email("root-url@example.com").build();
		entityManager.persist(member);
	}

	@ParameterizedTest
	@CsvSource({
		"https://example.com/, example.com",
		"https://example.com, example.com/",
		"https://example.com/?q=a#section, https://example.com?q=a#section"
	})
	@DisplayName("기존 루트 표기를 변경하지 않고 조회와 생성에서 동일한 중복을 찾는다")
	void rejectExistingRootVariant(String savedUrl, String input) {
		Link existing = Link.create(member, savedUrl, "기존 링크", null, null);
		entityManager.persist(existing);
		entityManager.flush();

		assertThat(linkService.findLinkIdByUrl(member, input)).contains(existing.getId());
		assertThatThrownBy(() -> linkService.createLink(member, input, "새 링크", null, null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.DUPLICATE_URL);
		assertThat(existing.getUrl()).isEqualTo(savedUrl);
	}

	@Test
	@DisplayName("신규 저장값에도 루트 정규화를 적용한다")
	void saveCanonicalRootUrl() {
		Link saved = linkService.createLink(member, "HTTPS://EXAMPLE.COM/", "새 링크", null, null);
		entityManager.flush();
		entityManager.clear();

		assertThat(entityManager.find(Link.class, saved.getId()).getUrl()).isEqualTo("https://example.com");
		assertThat(linkService.findLinkIdByUrl(member, "example.com/")).contains(saved.getId());
	}

	@Test
	@DisplayName("다른 회원의 링크와 삭제된 링크는 중복 대상에서 제외한다")
	void excludeOtherMemberAndDeletedLinks() {
		Member other = Member.builder().email("other-root@example.com").build();
		entityManager.persist(other);
		entityManager.persist(Link.create(other, "https://example.com/", "다른 회원", null, null));
		Link deleted = Link.create(member, "https://example.com/", "삭제됨", null, null);
		deleted.markDeleted();
		entityManager.persist(deleted);
		entityManager.flush();

		assertThat(linkService.findLinkIdByUrl(member, "example.com")).isEmpty();
		assertThat(linkService.createLink(member, "example.com/", "새 링크", null, null).getId()).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"http://example.com", "https://www.example.com", "https://example.com?q=a"})
	@DisplayName("프로토콜과 www 및 쿼리가 다른 URL을 임의로 합치지 않는다")
	void preserveDistinctUrls(String input) {
		entityManager.persist(Link.create(member, "https://example.com/", "기존 링크", null, null));
		entityManager.flush();

		assertThat(linkService.findLinkIdByUrl(member, input)).isEmpty();
		assertThat(linkService.createLink(member, input, "새 링크", null, null).getUrl()).isEqualTo(input);
	}

	@Test
	@DisplayName("일반 경로는 끝 슬래시가 다르면 별개로 유지한다")
	void preservePathSlash() {
		entityManager.persist(Link.create(member, "https://example.com/path/", "기존 링크", null, null));
		entityManager.flush();

		assertThat(linkService.findLinkIdByUrl(member, "https://example.com/path")).isEmpty();
		assertThat(linkService.createLink(member, "https://example.com/path", "새 링크", null, null).getId())
			.isNotNull();
	}
}
