package com.sofa.linkiving.domain.link.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

class UrlNormalizerTest {

	private final UrlNormalizer urlNormalizer = new UrlNormalizer();

	@ParameterizedTest
	@CsvSource({
		"naver.com, https://naver.com",
		"naver.com/, https://naver.com",
		"www.naver.com/, https://www.naver.com",
		"http://naver.com/, http://naver.com",
		"'https://naver.com/?q=abc#section', 'https://naver.com?q=abc#section'",
		"https://naver.com/path/, https://naver.com/path/",
		"https://naver.com//, https://naver.com//",
		"www.naver.com, https://www.naver.com",
		"https://www.naver.com, https://www.naver.com",
		"http://old-site.com, http://old-site.com",
		"https://https://naver.com, https://naver.com",
		"' https://naver.com ', https://naver.com",
		"'https://naver.com/path?q=abc#section', 'https://naver.com/path?q=abc#section'",
		"Naver.com, https://naver.com",
		"HTTPS://NAVER.COM, https://naver.com",
		"'https://Naver.com/Path?Q=Abc#Frag', 'https://naver.com/Path?Q=Abc#Frag'"
	})
	@DisplayName("URL을 규칙에 맞게 정규화한다")
	void normalizeUrl(String input, String expected) {
		assertThat(urlNormalizer.normalize(input)).isEqualTo(expected);
		assertThat(urlNormalizer.normalize(expected)).isEqualTo(expected);
	}

	@Test
	@DisplayName("루트 URL의 과거 슬래시 표기를 쿼리와 fragment 앞에서 비교한다")
	void compareLegacyRootUrl() {
		assertThat(urlNormalizer.comparisonUrls("HTTPS://NAVER.COM/?q=%2F#section"))
			.containsExactly("https://naver.com?q=%2F#section", "https://naver.com/?q=%2F#section",
				"https://www.naver.com?q=%2F#section", "https://www.naver.com/?q=%2F#section");
	}

	@Test
	@DisplayName("일반 경로의 슬래시는 비교 대상에 추가하거나 제거하지 않는다")
	void preserveNonRootPath() {
		assertThat(urlNormalizer.comparisonUrls("https://naver.com/path/"))
			.containsExactly("https://naver.com/path/", "https://www.naver.com/path/");
	}

	@ParameterizedTest
	@ValueSource(strings = {"naver.com", "naver.com/", "www.naver.com", "www.naver.com/"})
	@DisplayName("www와 루트 슬래시의 네 가지 입력은 동일한 저장값과 비교 순서를 사용한다")
	void compareWwwRootVariants(String input) {
		assertThat(urlNormalizer.comparisonUrls(input))
			.containsExactly("https://naver.com", "https://naver.com/",
				"https://www.naver.com", "https://www.naver.com/");
	}

	@ParameterizedTest
	@CsvSource({
		"https://www.example.com/Path?q=%2F#Frag, https://example.com/Path?q=%2F#Frag",
		"http://www.example.com:8080/path/, http://example.com:8080/path/",
		"https://user%40name@www.example.com:8443/, https://user%40name@example.com:8443",
		"https://www.blog.example.com/article, https://blog.example.com/article",
		"https://www.www.example.com/, https://example.com",
		"https://www.com/, https://www.com"
	})
	@DisplayName("저장 기준은 선행 www만 제거하고 나머지 URL 구성과 재적용 결과를 유지한다")
	void canonicalizeWwwHost(String input, String expected) {
		assertThat(urlNormalizer.comparisonUrls(input).get(0)).isEqualTo(expected);
		assertThat(urlNormalizer.comparisonUrls(expected).get(0)).isEqualTo(expected);
	}

	@Test
	@DisplayName("저장 기준과 달리 메타 수집에 사용할 접속 호스트는 www를 유지한다")
	void preserveFetchHost() {
		String input = "HTTPS://WWW.EXAMPLE.COM/article";
		assertThat(urlNormalizer.normalize(input)).isEqualTo("https://www.example.com/article");
		assertThat(urlNormalizer.comparisonUrls(input).get(0)).isEqualTo("https://example.com/article");
	}

	@Test
	@DisplayName("앞뒤 줄바꿈을 제거한다")
	void removeLeadingAndTrailingLineBreaks() {
		assertThat(urlNormalizer.normalize("\nhttps://naver.com\r\n"))
			.isEqualTo("https://naver.com");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "  ", "안녕하세요", "https://localhost", "https://naver"})
	@DisplayName("빈 값이거나 점이 없는 도메인은 거부한다")
	void rejectInvalidUrl(String input) {
		assertThatThrownBy(() -> urlNormalizer.normalize(input))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL);
	}

	@Test
	@DisplayName("null URL은 거부한다")
	void rejectNullUrl() {
		assertThatThrownBy(() -> urlNormalizer.normalize(null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL);
	}

	@Test
	@DisplayName("HTTP와 HTTPS 외의 프로토콜은 거부한다")
	void rejectUnsupportedProtocol() {
		assertThatThrownBy(() -> urlNormalizer.normalize("ftp://example.com"))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PROTOCOL);
	}
}
