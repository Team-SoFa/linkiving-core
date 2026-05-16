package com.sofa.linkiving.global.config.jackson;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.sofa.linkiving.global.util.HashidsUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("HashidsDeserializer 단위 테스트")
class HashidsDeserializerTest {

	@InjectMocks
	private HashidsDeserializer hashidsDeserializer;

	@Mock
	private HashidsUtils hashidsUtils;

	@Mock
	private JsonParser jsonParser;

	@Mock
	private DeserializationContext deserializationContext;

	@BeforeEach
	void setUp() {
		hashidsDeserializer.setHashidsUtils(hashidsUtils);
	}

	@Test
	@DisplayName("정상적인 해시 문자열이 주어지면 디코딩하여 Long 값으로 반환한다")
	void deserialize_Success() throws IOException {
		// given
		String encodedHash = "abc123de";
		Long expectedDecodedValue = 123L;

		given(jsonParser.getValueAsString()).willReturn(encodedHash);
		given(hashidsUtils.decode(encodedHash)).willReturn(expectedDecodedValue);

		// when
		Long result = hashidsDeserializer.deserialize(jsonParser, deserializationContext);

		// then
		assertThat(result).isEqualTo(expectedDecodedValue);
		verify(hashidsUtils, times(1)).decode(encodedHash);
	}

	@Test
	@DisplayName("해시 문자열이 null이거나 비어있으면 null을 반환한다")
	void deserialize_NullOrBlank() throws IOException {
		// given
		given(jsonParser.getValueAsString()).willReturn("   ");

		// when
		Long result = hashidsDeserializer.deserialize(jsonParser, deserializationContext);

		// then
		assertThat(result).isNull();
		verify(hashidsUtils, never()).decode(anyString());
	}
}
