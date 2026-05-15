package com.sofa.linkiving.global.config.jackson;

import static org.mockito.BDDMockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sofa.linkiving.global.util.HashidsUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("HashidsSerializer 단위 테스트")
class HashidsSerializerTest {

	@InjectMocks
	private HashidsSerializer hashidsSerializer;

	@Mock
	private HashidsUtils hashidsUtils;

	@Mock
	private JsonGenerator jsonGenerator;

	@Mock
	private SerializerProvider serializerProvider;

	@BeforeEach
	void setUp() {
		hashidsSerializer.setHashidsUtils(hashidsUtils);
	}

	@Test
	@DisplayName("정상적인 Long 값이 주어지면 Hashids로 인코딩하여 JSON 문자열로 쓴다")
	void serialize_Success() throws IOException {
		// given
		Long value = 123L;
		String encodedHash = "abc123de";
		given(hashidsUtils.encode(value)).willReturn(encodedHash);

		// when
		hashidsSerializer.serialize(value, jsonGenerator, serializerProvider);

		// then
		verify(jsonGenerator, times(1)).writeString(encodedHash);
	}

	@Test
	@DisplayName("null 값이 주어지면 JSON null을 쓴다")
	void serialize_Null() throws IOException {
		// when
		hashidsSerializer.serialize(null, jsonGenerator, serializerProvider);

		// then
		verify(jsonGenerator, times(1)).writeNull();
		verify(hashidsUtils, never()).encode(any());
	}
}
