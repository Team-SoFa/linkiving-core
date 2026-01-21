package com.sofa.linkiving.infra.s3;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.link.util.UrlValidator;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
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
	private URLConnection mockConnection;
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

		// 1. 중복 검사 통과 (S3에 파일 없음)
		given(s3Template.objectExists(eq(BUCKET_NAME), anyString())).willReturn(false);

		willDoNothing().given(urlValidator).validateSafeUrl(originalUrl);

		// 2. Factory가 Mock Connection 반환
		given(urlConnectionFactory.createConnection(originalUrl)).willReturn(mockConnection);

		// 3. Connection 동작 설정 (이미지 타입, 스트림)
		given(mockConnection.getContentType()).willReturn("image/jpeg");
		given(mockConnection.getInputStream()).willReturn(new ByteArrayInputStream("dummy-data".getBytes()));

		// when
		String result = s3ImageUploader.uploadFromUrl(originalUrl);

		// then
		// 예상되는 S3 URL 포맷 확인
		// UUID 생성이 내부 로직이라 정확한 키값 예측은 어려우나, bucket/region/경로 포함 여부 확인
		assertThat(result).startsWith("https://" + BUCKET_NAME + ".s3." + REGION + ".amazonaws.com/links/");
		assertThat(result).endsWith(".jpg");

		// 실제 업로드가 수행되었는지 검증
		verify(s3Template).upload(eq(BUCKET_NAME), anyString(), any(InputStream.class), isNull());
	}

	@Test
	@DisplayName("이미 S3에 동일한 이미지가 존재하면(Cache Hit) 업로드 없이 S3 URL을 반환한다")
	void shouldReturnS3UrlWhenImageExists() throws IOException {
		// given
		String originalUrl = "https://example.com/image.jpg";

		// 이미 파일이 존재한다고 설정 (Cache Hit)
		given(s3Template.objectExists(eq(BUCKET_NAME), anyString())).willReturn(true);

		// when
		String result = s3ImageUploader.uploadFromUrl(originalUrl);

		// then
		assertThat(result).contains(BUCKET_NAME, REGION, "links/");

		// Factory 연결 생성 및 Upload가 호출되지 않아야 함
		verify(urlConnectionFactory, never()).createConnection(anyString());
		verify(s3Template, never()).upload(anyString(), anyString(), any(InputStream.class), any());
	}

	@Test
	@DisplayName("ContentType이 이미지가 아닌 경우(예: HTML, PDF) Null을 반환한다")
	void shouldReturnDefaultImageUrlWhenNotImage() throws IOException {
		// given
		String originalUrl = "https://example.com/document.pdf";

		given(s3Template.objectExists(eq(BUCKET_NAME), anyString())).willReturn(false);
		willDoNothing().given(urlValidator).validateSafeUrl(originalUrl);
		given(urlConnectionFactory.createConnection(originalUrl)).willReturn(mockConnection);

		// ContentType을 이미지가 아닌 것으로 설정
		given(mockConnection.getContentType()).willReturn("application/pdf");

		// when
		String result = s3ImageUploader.uploadFromUrl(originalUrl);

		// then
		assertThat(result).isNull();

		// Upload 호출 안 됨
		verify(s3Template, never()).upload(anyString(), anyString(), any(InputStream.class), any());
	}

	@Test
	@DisplayName("연결 실패나 업로드 중 예외 발생 시 Null을 반환한다 (Fallback)")
	void shouldReturnDefaultImageUrlWhenExceptionOccurs() throws IOException {
		// given
		String originalUrl = "https://example.com/image.jpg";

		given(s3Template.objectExists(eq(BUCKET_NAME), anyString())).willReturn(false);
		willDoNothing().given(urlValidator).validateSafeUrl(originalUrl);

		// 연결 생성 시 예외 발생하도록 설정
		given(urlConnectionFactory.createConnection(originalUrl)).willThrow(new IOException("Connection Refused"));

		// when
		String result = s3ImageUploader.uploadFromUrl(originalUrl);

		// then
		assertThat(result).isNull();
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
	@DisplayName("검증 실패 시 기본 이미지 URL을 반환한다")
	void shouldReturnDefaultImageUrlWhenValidationFails() {
		// given
		String originalUrl = "http://127.0.0.1/image.jpg";

		willThrow(new BusinessException(LinkErrorCode.INVALID_URL_PRIVATE_IP))
			.given(urlValidator).validateSafeUrl(originalUrl);

		// when
		String result = s3ImageUploader.uploadFromUrl(originalUrl);

		// then
		assertThat(result).isEqualTo(DEFAULT_IMAGE_URL);
		verify(urlConnectionFactory, never()).createConnection(anyString());
		verify(s3Template, never()).upload(anyString(), anyString(), any(InputStream.class), any());
	}

	@Test
	@DisplayName("저장소 URL이면 캐시 확인 후 바로 반환한다")
	void shouldResolveStoredUrlWhenS3UrlProvided() {
		// given
		String s3Url = "https://" + BUCKET_NAME + ".s3." + REGION + ".amazonaws.com/links/test.jpg";

		given(s3Template.objectExists(BUCKET_NAME, "links/test.jpg")).willReturn(true);

		// when
		String result = s3ImageUploader.resolveStoredUrl(s3Url);

		// then
		assertThat(result).isEqualTo(s3Url);
	}
}
