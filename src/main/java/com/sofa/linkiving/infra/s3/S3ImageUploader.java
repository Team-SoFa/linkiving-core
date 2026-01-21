package com.sofa.linkiving.infra.s3;

import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.abstraction.ImageUploader;
import com.sofa.linkiving.domain.link.util.UrlValidator;

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

	@Override
	public String uploadFromUrl(String originalUrl) {
		if (originalUrl == null || originalUrl.isBlank()) {
			return null;
		}

		try {
			String cachedUrl = resolveStoredUrl(originalUrl);
			if (cachedUrl != null) {
				return cachedUrl;
			}

			urlValidator.validateSafeUrl(originalUrl);

			String s3Key = generateUniqueKeyFromUrl(originalUrl);
			String s3Url = buildS3Url(s3Key);

			URLConnection connection = urlConnectionFactory.createConnection(originalUrl);
			connection.setConnectTimeout(3000);
			connection.setReadTimeout(3000);

			String contentType = connection.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				log.warn("Not Image: {}", originalUrl);
				return null;
			}

			try (InputStream inputStream = connection.getInputStream()) {
				s3Template.upload(bucketName, s3Key, inputStream, null);
				log.info("Image uploaded: {}", s3Key);
				return s3Url;
			}

		} catch (Exception e) {
			log.warn("Image upload failed: {}", e.getMessage());
			return null;
		}
	}

	@Override
	public String resolveStoredUrl(String originalUrl) {
		if (originalUrl == null || originalUrl.isBlank()) {
			return null;
		}

		try {
			String storedKey = extractStoredKey(originalUrl);
			if (storedKey != null) {
				if (s3Template.objectExists(bucketName, storedKey)) {
					return originalUrl;
				}
				return null;
			}

			String s3Key = generateUniqueKeyFromUrl(originalUrl);
			if (s3Template.objectExists(bucketName, s3Key)) {
				log.info("Image already exists (Cache Hit): {} -> {}", originalUrl, s3Key);
				return buildS3Url(s3Key);
			}

			return null;
		} catch (Exception e) {
			log.warn("Image cache lookup failed: {}", e.getMessage());
			return null;
		}
	}

	private String buildS3Url(String key) {
		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
	}

	private String extractStoredKey(String url) {
		String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
		if (!url.startsWith(prefix)) {
			return null;
		}
		String key = url.substring(prefix.length());
		return key.isBlank() ? null : key;
	}

	private String generateUniqueKeyFromUrl(String url) {
		try {
			String extension = "jpg";
			int lastDotIndex = url.lastIndexOf('.');
			if (lastDotIndex > 0 && lastDotIndex < url.length() - 1) {
				String ext = url.substring(lastDotIndex + 1);
				if (ext.contains("?")) {
					ext = ext.substring(0, ext.indexOf("?"));
				}
				if (ext.length() <= 4 && ext.matches("[a-zA-Z]+")) {
					extension = ext;
				}
			}
			UUID uuid = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
			return "links/" + uuid + "." + extension;
		} catch (Exception e) {
			return "links/" + UUID.randomUUID() + ".jpg";
		}
	}
}
