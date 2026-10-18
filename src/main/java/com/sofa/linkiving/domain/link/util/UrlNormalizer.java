package com.sofa.linkiving.domain.link.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

@Component
public class UrlNormalizer {

	private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://");
	private static final Pattern DUPLICATED_HTTP_SCHEME_PATTERN = Pattern.compile(
		"^(https?://)(?:https?://)+",
		Pattern.CASE_INSENSITIVE
	);

	public String normalize(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			throw new BusinessException(LinkErrorCode.INVALID_URL);
		}

		String normalizedUrl = removeDuplicatedHttpScheme(rawUrl.strip());
		if (!SCHEME_PATTERN.matcher(normalizedUrl).find()) {
			normalizedUrl = "https://" + normalizedUrl;
		}

		return normalizeSchemeAndHost(normalizedUrl);
	}

	/** 정규화된 저장값을 먼저 반환하고, 루트 URL은 과거 슬래시 표기도 함께 비교한다. */
	public List<String> comparisonUrls(String rawUrl) {
		String normalizedUrl = normalize(rawUrl);
		URI uri = URI.create(normalizedUrl);
		if (!uri.getRawPath().isEmpty()) {
			return List.of(normalizedUrl);
		}

		int pathStart = uri.getScheme().length() + 3 + uri.getRawAuthority().length();
		String legacyRootUrl = normalizedUrl.substring(0, pathStart) + "/" + normalizedUrl.substring(pathStart);
		return List.of(normalizedUrl, legacyRootUrl);
	}

	private String removeDuplicatedHttpScheme(String url) {
		Matcher matcher = DUPLICATED_HTTP_SCHEME_PATTERN.matcher(url);
		return matcher.find() ? matcher.replaceFirst(matcher.group(1)) : url;
	}

	private String normalizeSchemeAndHost(String url) {
		try {
			URI uri = new URI(url);
			String scheme = uri.getScheme();
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				throw new BusinessException(LinkErrorCode.INVALID_URL_PROTOCOL);
			}

			String host = uri.getHost();
			if (host == null || host.isBlank() || !host.contains(".")) {
				throw new BusinessException(LinkErrorCode.INVALID_URL);
			}

			return rebuildUrl(uri, scheme.toLowerCase(Locale.ROOT), host.toLowerCase(Locale.ROOT));
		} catch (URISyntaxException exception) {
			throw new BusinessException(LinkErrorCode.INVALID_URL);
		}
	}

	private String rebuildUrl(URI uri, String scheme, String host) {
		String authority = uri.getRawAuthority();
		int hostStart = authority.lastIndexOf('@') + 1;
		int hostEnd = authority.indexOf(':', hostStart);
		if (hostEnd == -1) {
			hostEnd = authority.length();
		}

		String normalizedAuthority = authority.substring(0, hostStart)
			+ host
			+ authority.substring(hostEnd);
		StringBuilder normalizedUrl = new StringBuilder(scheme)
			.append("://")
			.append(normalizedAuthority)
			.append("/".equals(uri.getRawPath()) ? "" : uri.getRawPath());

		if (uri.getRawQuery() != null) {
			normalizedUrl.append('?').append(uri.getRawQuery());
		}
		if (uri.getRawFragment() != null) {
			normalizedUrl.append('#').append(uri.getRawFragment());
		}
		return normalizedUrl.toString();
	}
}
