package com.sofa.linkiving.global.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sofa.linkiving.global.error.exception.BusinessException;

@DisplayName("HashidsUtils 단위 테스트")
class HashidsUtilsTest {

	private HashidsUtils hashidsUtils;

	@BeforeEach
	void setUp() {
		hashidsUtils = new HashidsUtils("test-salt", 8);
	}

	@Test
	@DisplayName("Long 타입 ID를 해시 문자열로 인코딩한다")
	void encode_Success() {
		// given
		Long id = 123L;

		// when
		String encoded = hashidsUtils.encode(id);

		// then
		assertThat(encoded).isNotNull();
		assertThat(encoded).hasSizeGreaterThanOrEqualTo(8);
	}

	@Test
	@DisplayName("null을 인코딩하려고 하면 null을 반환한다")
	void encode_Null() {
		// when & then
		assertThat(hashidsUtils.encode(null)).isNull();
	}

	@Test
	@DisplayName("해시 문자열을 다시 원래의 Long 타입 ID로 디코딩한다")
	void decode_Success() {
		// given
		Long originalId = 123L;
		String encoded = hashidsUtils.encode(originalId);

		// when
		Long decoded = hashidsUtils.decode(encoded);

		// then
		assertThat(decoded).isEqualTo(originalId);
	}

	@Test
	@DisplayName("null이거나 빈 문자열을 디코딩하면 null을 반환한다")
	void decode_NullOrBlank() {
		// when & then
		assertThat(hashidsUtils.decode(null)).isNull();
		assertThat(hashidsUtils.decode("")).isNull();
		assertThat(hashidsUtils.decode("   ")).isNull();
	}

	@Test
	@DisplayName("유효하지 않은 해시 문자열 디코딩 시 예외가 발생한다")
	void decode_Fail_InvalidHash() {
		// when & then
		assertThatThrownBy(() -> hashidsUtils.decode("invalidHash!@#"))
			.isInstanceOf(BusinessException.class)
			.hasMessage("유효하지 않은 식별자입니다.");
	}
}
