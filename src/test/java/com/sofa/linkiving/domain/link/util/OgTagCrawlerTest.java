package com.sofa.linkiving.domain.link.util;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class OgTagCrawlerTest {

	@Test
	@DisplayName("메타데이터 수집 전 정규화된 URL을 SSRF 검증한다")
	void validateNormalizedUrlBeforeCrawling() {
		UrlValidator urlValidator = mock(UrlValidator.class);
		UrlNormalizer urlNormalizer = mock(UrlNormalizer.class);
		OgTagCrawler ogTagCrawler = new OgTagCrawler(urlValidator, urlNormalizer);
		String rawUrl = " 192.168.0.1 ";
		String normalizedUrl = "https://192.168.0.1";

		given(urlNormalizer.normalize(rawUrl)).willReturn(normalizedUrl);
		willThrow(new BusinessException(LinkErrorCode.INVALID_URL_PRIVATE_IP))
			.given(urlValidator).validateSafeUrl(normalizedUrl);

		assertThatThrownBy(() -> ogTagCrawler.crawl(rawUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PRIVATE_IP);

		then(urlNormalizer).should().normalize(rawUrl);
		then(urlValidator).should().validateSafeUrl(normalizedUrl);
	}

	@Test
	@DisplayName("정규화된 URL로 메타데이터를 요청한다")
	void crawlNormalizedUrl() throws IOException, InterruptedException {
		try (MockWebServer mockWebServer = new MockWebServer()) {
			mockWebServer.enqueue(new MockResponse().setBody("""
				<html>
				<head><meta property="og:title" content="normalized target"></head>
				</html>
				"""));
			mockWebServer.start();

			UrlValidator urlValidator = mock(UrlValidator.class);
			UrlNormalizer urlNormalizer = mock(UrlNormalizer.class);
			OgTagCrawler ogTagCrawler = new OgTagCrawler(urlValidator, urlNormalizer);
			String rawUrl = "naver.com";
			String normalizedUrl = mockWebServer.url("/article?Q=Abc").toString();
			given(urlNormalizer.normalize(rawUrl)).willReturn(normalizedUrl);

			assertThat(ogTagCrawler.crawl(rawUrl).title()).isEqualTo("normalized target");
			RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
			assertThat(request).isNotNull();
			assertThat(request.getPath()).isEqualTo("/article?Q=Abc");
			then(urlValidator).should().validateSafeUrl(normalizedUrl);
		}
	}
}
