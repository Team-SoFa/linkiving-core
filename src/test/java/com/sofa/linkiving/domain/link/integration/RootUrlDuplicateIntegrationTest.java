package com.sofa.linkiving.domain.link.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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

	private static Stream<Arguments> rootUrlPairs() {
		List<String> savedUrls = List.of("https://example.com", "https://example.com/",
			"https://www.example.com", "https://www.example.com/");
		List<String> inputs = List.of("example.com", "example.com/", "www.example.com", "www.example.com/");
		return savedUrls.stream().flatMap(saved -> inputs.stream().map(input -> Arguments.of(saved, input)));
	}

	@BeforeEach
	void setUp() {
		member = Member.builder().email("root-url@example.com").build();
		entityManager.persist(member);
	}

	@ParameterizedTest
	@MethodSource("rootUrlPairs")
	@DisplayName("www와 루트 슬래시의 기존 표기 모두 조회와 생성에서 같은 중복으로 인식한다")
	void rejectAllRootVariants(String savedUrl, String input) {
		assertExistingDuplicate(savedUrl, input);
	}

	@ParameterizedTest
	@CsvSource({
		"https://www.example.com/?q=a#section, https://example.com?q=a#section",
		"https://example.com?q=a#section, https://www.example.com/?q=a#section",
		"https://www.example.com/Path?q=%2F#Frag, https://example.com/Path?q=%2F#Frag",
		"https://example.com/path/, https://www.example.com/path/"
	})
	@DisplayName("경로·쿼리·fragment가 있는 기존 www 표기도 동일한 중복으로 찾는다")
	void rejectExistingWwwVariant(String savedUrl, String input) {
		assertExistingDuplicate(savedUrl, input);
	}

	private void assertExistingDuplicate(String savedUrl, String input) {
		Link existing = Link.create(member, savedUrl, "기존 링크", null, null);
		entityManager.persist(existing);
		entityManager.flush();

		assertThat(linkService.findLinkIdByUrl(member, input)).contains(existing.getId());
		assertThatThrownBy(() -> linkService.createLink(member, input, "새 링크", null, null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.DUPLICATE_URL);
		assertThat(existing.getUrl()).isEqualTo(savedUrl);
	}

	@ParameterizedTest
	@CsvSource({
		"example.com, https://example.com",
		"example.com/, https://example.com",
		"www.example.com, https://example.com",
		"HTTPS://WWW.EXAMPLE.COM/, https://example.com",
		"https://www.example.com/Path?q=%2F#Frag, https://example.com/Path?q=%2F#Frag",
		"http://www.example.com:8080/path/, http://example.com:8080/path/"
	})
	@DisplayName("신규 DB 저장값에도 www와 루트 슬래시 정규화를 적용한다")
	void saveCanonicalUrl(String input, String expected) {
		Link saved = linkService.createLink(member, input, "새 링크", null, null);
		entityManager.flush();
		entityManager.clear();

		assertThat(entityManager.find(Link.class, saved.getId()).getUrl()).isEqualTo(expected);
		assertThat(linkService.findLinkIdByUrl(member, input)).contains(saved.getId());
	}

	@Test
	@DisplayName("다른 회원의 링크와 삭제된 링크는 중복 대상에서 제외한다")
	void excludeOtherMemberAndDeletedLinks() {
		Member other = Member.builder().email("other-root@example.com").build();
		entityManager.persist(other);
		entityManager.persist(Link.create(other, "https://www.example.com/", "다른 회원", null, null));
		Link deleted = Link.create(member, "https://www.example.com/", "삭제됨", null, null);
		deleted.markDeleted();
		entityManager.persist(deleted);
		entityManager.flush();

		assertThat(linkService.findLinkIdByUrl(member, "example.com")).isEmpty();
		assertThat(linkService.createLink(member, "example.com/", "새 링크", null, null).getId()).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"http://example.com", "https://m.example.com", "https://blog.example.com",
		"https://example.com:8443", "https://example.com?q=a", "https://example.com#section"})
	@DisplayName("프로토콜과 다른 서브도메인 및 포트·쿼리·fragment가 다른 URL은 구분한다")
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

	@Test
	@DisplayName("기존에 중복 저장된 표기가 모두 있어도 동일한 기존 링크를 우선 조회하고 데이터를 유지한다")
	void preferCanonicalExistingLink() {
		Link legacy = Link.create(member, "https://www.example.com/", "www 기존 링크", null, null);
		Link canonical = Link.create(member, "https://example.com", "기존 링크", null, null);
		entityManager.persist(legacy);
		entityManager.persist(canonical);
		entityManager.flush();

		for (String input : List.of("example.com", "example.com/", "www.example.com", "www.example.com/")) {
			assertThat(linkService.findLinkIdByUrl(member, input)).contains(canonical.getId());
		}
		assertThat(linkService.getLinkTotalCount(member)).isEqualTo(2);
		assertThat(legacy.getUrl()).isEqualTo("https://www.example.com/");
	}
}
