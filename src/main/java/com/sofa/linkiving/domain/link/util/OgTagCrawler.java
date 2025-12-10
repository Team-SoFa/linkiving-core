package com.sofa.linkiving.domain.link.util;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.OgTagDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OgTagCrawler {

	private static final int TIMEOUT_MS = 5000;

	public OgTagDto crawl(String url) {
		try {
			Document document = Jsoup.connect(url)
				.timeout(TIMEOUT_MS)
				.userAgent("Mozilla/5.0")
				.get();

			return OgTagDto.builder()
				.title(getMetaTag(document, "og:title"))
				.description(getMetaTag(document, "og:description"))
				.image(getMetaTag(document, "og:image"))
				.url(getMetaTag(document, "og:url"))
				.build();

		} catch (IOException e) {
			log.warn("OG 태그 크롤링 실패: {}", url, e);
			return OgTagDto.empty();
		}
	}

	private String getMetaTag(Document document, String property) {
		return document.select("meta[property=" + property + "]")
			.attr("content");
	}
}
