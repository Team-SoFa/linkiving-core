package com.sofa.linkiving.infra.s3;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.util.UrlValidator;
import com.sofa.linkiving.global.error.exception.BusinessException;

import io.awspring.cloud.s3.S3Template;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3ImageUploader 단위 테스트")
public class S3ImageUploaderTest {

	private static final String BUCKET_NAME = "test-bucket";
	private static final String REGION = "ap-northeast-2";
	private static final String DEFAULT_IMAGE_URL = "https://example.com/default-image.jpg";

	@InjectMocks
	private S3ImageUploader s3ImageUploader;

	@Mock
	private S3Template s3Template;

	@Mock
	private UrlConnectionFactory urlConnectionFactory;

	@Mock
	private UrlValidator urlValidator;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(s3ImageUploader, "bucketName", BUCKET_NAME);
		ReflectionTestUtils.setField(s3ImageUploader, "region", REGION);
		ReflectionTestUtils.setField(s3ImageUploader, "defaultImageUrl", DEFAULT_IMAGE_URL);
	}

	@Test
	@DisplayName("정상적인 이미지 URL인 경우 S3에 업로드하고 생성된 S3 URL을 반환한다")
	void shouldUploadImageWhenUrlIsValid() throws IOException {
		// given
		String originalUrl = "https://example.com/image.jpg";

		given(s3Template.objectExists(eq(BUCKET_NAME), anyString())).willReturn(false);
		willDoNothing().given(urlValidator).validateSafeUrl(originalUrl);

		InputStream inputStream = new ByteArrayInputStream("dummy-data".getBytes());
		given(urlConnectionFactory.openStream(originalUrl)).willReturn(inputStream);

		// when
		String result = s3ImageUploader.uploadFromUrl(originalUrl);

		// then
		assertThat(result).contains("images/");
		assertThat(result).contains(BUCKET_NAME + ".s3." + REGION);
		verify(s3Template).upload(eq(BUCKET_NAME), anyString(), any(InputStream.class), any());
	}

	@Test
	@DisplayName("이미 S3에 동일한 이미지가 존재하면(Cache Hit) 업로드 없이 S3 URL을 반환한다")
	void shouldReturnS3UrlWhenImageExists() throws IOException {
		// given
		String originalUrl = "https://example.com/image.jpg";
		given(s3Template.objectExists(eq(BUCKET_NAME), anyString())).willReturn(true);

		// when
		String result = s3ImageUploader.uploadFromUrl(originalUrl);

		// then
		assertThat(result).contains("images/");
		verify(urlConnectionFactory, never()).openStream(anyString());
		verify(s3Template, never()).upload(anyString(), anyString(), any(InputStream.class), any());
	}

	@Test
	@DisplayName("업로드 중 예외 발생 시 기본 이미지 URL을 반환한다")
	void shouldReturnDefaultImageUrlWhenExceptionOccurs() throws IOException {
		// given
		String originalUrl = "https://example.com/image.jpg";
		given(s3Template.objectExists(eq(BUCKET_NAME), anyString())).willReturn(false);
		willDoNothing().given(urlValidator).validateSafeUrl(originalUrl);

		given(urlConnectionFactory.openStream(originalUrl)).willThrow(new IOException("Connection Refused"));

		// when
		String result = s3ImageUploader.uploadFromUrl(originalUrl);

		// then
		assertThat(result).isEqualTo(DEFAULT_IMAGE_URL);
	}

	@Test
	@DisplayName("입력 URL이 null이거나 비어있으면 null을 반환한다")
	void shouldReturnNullWhenUrlIsEmpty() {
		// when
		String resultNull = s3ImageUploader.uploadFromUrl(null);
		String resultEmpty = s3ImageUploader.uploadFromUrl("");

		// then
		assertThat(resultNull).isNull();
		assertThat(resultEmpty).isNull();
	}

	@Test
	@DisplayName("검증 실패(BusinessException) 시 기본 이미지 URL을 반환한다")
	void shouldReturnDefaultImageUrlWhenValidationFails() throws IOException {
		// given
		String originalUrl = "http://127.0.0.1/image.jpg";
		willThrow(new BusinessException(LinkErrorCode.INVALID_URL_PRIVATE_IP))
			.given(urlValidator).validateSafeUrl(originalUrl);

		// when
		String result = s3ImageUploader.uploadFromUrl(originalUrl);

		// then
		assertThat(result).isEqualTo(DEFAULT_IMAGE_URL);
		verify(urlConnectionFactory, never()).openStream(anyString());
	}

	@Test
	@DisplayName("이미 존재하는 파일인지 확인하여 Optional URL을 반환한다")
	void shouldResolveStoredUrl() {
		// given
		String originalUrl = "https://example.com/test.jpg";
		given(s3Template.objectExists(eq(BUCKET_NAME), anyString())).willReturn(true);

		// when
		Optional<String> result = s3ImageUploader.resolveStoredUrl(originalUrl);

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).contains("images/");
	}
}
