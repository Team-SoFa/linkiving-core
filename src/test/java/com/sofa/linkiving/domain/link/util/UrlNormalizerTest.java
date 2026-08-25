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
