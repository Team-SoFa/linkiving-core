package com.sofa.linkiving.infra.s3;

import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.abstraction.ImageUploader;
import com.sofa.linkiving.domain.link.util.UrlValidator;
import com.sofa.linkiving.global.error.exception.BusinessException;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageUploader implements ImageUploader {

	private final S3Template s3Template;
	private final UrlConnectionFactory urlConnectionFactory;
	private final UrlValidator urlValidator;

	@Value("${spring.cloud.aws.s3.bucket}")
	private String bucketName;

	@Value("${spring.cloud.aws.region.static}")
	private String region;

	@Value("${app.link.default-image-url:}")
	private String defaultImageUrl;

	@Override
	public String uploadFromUrl(String originalUrl) {
		if (originalUrl == null || originalUrl.isBlank()) {
			return null;
		}

		return resolveStoredUrl(originalUrl)
			.orElseGet(() -> uploadNewImage(originalUrl));
	}

	/**
	 * 신규 이미지를 S3에 업로드하는 내부 로직
	 */
	private String uploadNewImage(String originalUrl) {
		try {
			urlValidator.validateSafeUrl(originalUrl);
			String s3Key = generateUniqueKeyFromUrl(originalUrl);

			URLConnection connection = urlConnectionFactory.createConnection(originalUrl);
			connection.setConnectTimeout(3000);
			connection.setReadTimeout(3000);

			connection.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
					+ "AppleWebKit/537.36 (KHTML, like Gecko) "
					+ "Chrome/120.0.0.0 Safari/537.36");
			connection.setRequestProperty("Accept", "image/*, */*;q=0.8");

			String contentType = connection.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				log.warn("Not Image (ContentType: {}): {}", contentType, originalUrl);
				return normalizedDefaultImageUrl();
			}

			try (InputStream is = connection.getInputStream()) {
				s3Template.upload(bucketName, s3Key, is, ObjectMetadata.builder().contentType(contentType).build());

				return buildS3Url(s3Key);
			}
		} catch (BusinessException e) {
			log.warn("Invalid image URL skipping upload: {}", originalUrl);
			return normalizedDefaultImageUrl();
		} catch (Exception e) {
			log.error("S3 upload failed for URL: {}", originalUrl, e);
			return normalizedDefaultImageUrl();
		}
	}

	/**
	 * S3에 해당 파일이 이미 존재하는지 확인 (Optional 기반 캐시 체크)
	 */
	public Optional<String> resolveStoredUrl(String originalUrl) {
		try {
			String s3Key = generateUniqueKeyFromUrl(originalUrl);
			if (s3Template.objectExists(bucketName, s3Key)) {
				log.info("Image already exists (Cache Hit): {} -> {}", originalUrl, s3Key);
				return Optional.of(buildS3Url(s3Key));
			}
		} catch (Exception e) {
			log.warn("Image cache lookup failed: {}", e.getMessage());
		}
		return Optional.empty();
	}

	private String buildS3Url(String key) {
		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
	}

	private String normalizedDefaultImageUrl() {
		return (defaultImageUrl == null || defaultImageUrl.isBlank()) ? null : defaultImageUrl;
	}

	private String generateUniqueKeyFromUrl(String url) {
		try {
			String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
			String extension = extractExtension(decodedUrl);

			return "images/" + UUID.nameUUIDFromBytes(decodedUrl.getBytes()) + "." + extension;
		} catch (Exception e) {
			return "images/" + UUID.randomUUID() + ".jpg";
		}
	}

	private String extractExtension(String url) {
		String extension = "jpg";
		int lastDotIndex = url.lastIndexOf('.');
		if (lastDotIndex > 0 && lastDotIndex < url.length() - 1) {
			String ext = url.substring(lastDotIndex + 1);
			if (ext.contains("?")) {
				ext = ext.substring(0, ext.indexOf("?"));
			}
			if (ext.length() <= 4) {
				extension = ext.toLowerCase();
			}
		}
		return extension;
	}
}
