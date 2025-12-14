package com.sofa.linkiving.domain.link.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

class UrlValidatorTest {

	private final UrlValidator urlValidator = new UrlValidator();

	@Test
	@DisplayName("유효한 HTTP URL은 검증을 통과한다")
	void validateSafeUrl_ValidHttpUrl_Success() {
		// given
		String validUrl = "http://example.com";

		// when & then
		assertThatCode(() -> urlValidator.validateSafeUrl(validUrl))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("유효한 HTTPS URL은 검증을 통과한다")
	void validateSafeUrl_ValidHttpsUrl_Success() {
		// given
		String validUrl = "https://www.google.com";

		// when & then
		assertThatCode(() -> urlValidator.validateSafeUrl(validUrl))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("file 프로토콜은 차단된다")
	void validateSafeUrl_FileProtocol_ThrowsException() {
		// given
		String fileUrl = "file:///etc/passwd";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(fileUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PROTOCOL);
	}

	@Test
	@DisplayName("ftp 프로토콜은 차단된다")
	void validateSafeUrl_FtpProtocol_ThrowsException() {
		// given
		String ftpUrl = "ftp://example.com";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(ftpUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PROTOCOL);
	}

	@Test
	@DisplayName("localhost는 차단된다")
	void validateSafeUrl_Localhost_ThrowsException() {
		// given
		String localhostUrl = "http://localhost:8080";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(localhostUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PRIVATE_IP);
	}

	@Test
	@DisplayName("127.0.0.1은 차단된다")
	void validateSafeUrl_Loopback127_ThrowsException() {
		// given
		String loopbackUrl = "http://127.0.0.1:8080";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(loopbackUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PRIVATE_IP);
	}

	@Test
	@DisplayName("0.0.0.0은 차단된다")
	void validateSafeUrl_ZeroAddress_ThrowsException() {
		// given
		String zeroUrl = "http://0.0.0.0:8080";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(zeroUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PRIVATE_IP);
	}

	@Test
	@DisplayName("Private IP (10.x.x.x)는 차단된다")
	void validateSafeUrl_PrivateIp10_ThrowsException() {
		// given
		String privateUrl = "http://10.0.0.1";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(privateUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PRIVATE_IP);
	}

	@Test
	@DisplayName("Private IP (192.168.x.x)는 차단된다")
	void validateSafeUrl_PrivateIp192_ThrowsException() {
		// given
		String privateUrl = "http://192.168.1.1";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(privateUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PRIVATE_IP);
	}

	@Test
	@DisplayName("Private IP (172.16.x.x)는 차단된다")
	void validateSafeUrl_PrivateIp172_ThrowsException() {
		// given
		String privateUrl = "http://172.16.0.1";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(privateUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PRIVATE_IP);
	}

	@Test
	@DisplayName("AWS 메타데이터 서버 주소는 차단된다")
	void validateSafeUrl_AwsMetadata_ThrowsException() {
		// given
		String awsMetadataUrl = "http://169.254.169.254/latest/meta-data/";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(awsMetadataUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PRIVATE_IP);
	}

	@Test
	@DisplayName("잘못된 URL 형식은 예외가 발생한다")
	void validateSafeUrl_MalformedUrl_ThrowsException() {
		// given
		String malformedUrl = "not-a-valid-url";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(malformedUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL);
	}

	@Test
	@DisplayName("빈 URL은 예외가 발생한다")
	void validateSafeUrl_EmptyUrl_ThrowsException() {
		// given
		String emptyUrl = "";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(emptyUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL);
	}

	@Test
	@DisplayName("IPv6 loopback (::1)은 차단된다")
	void validateSafeUrl_IPv6Loopback_ThrowsException() {
		// given
		String ipv6LoopbackUrl = "http://[::1]:8080";

		// when & then
		assertThatThrownBy(() -> urlValidator.validateSafeUrl(ipv6LoopbackUrl))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", LinkErrorCode.INVALID_URL_PRIVATE_IP);
	}
}
