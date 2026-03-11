package com.sofa.linkiving.infra.s3;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultUrlConnectionFactory 단위 테스트")
class DefaultUrlConnectionFactoryTest {

	@Spy
	private DefaultUrlConnectionFactory factory;

	@Mock
	private URLConnection mockConnection;

	@Test
	@DisplayName("createConnection: 유효한 URL 문자열로 URLConnection 객체를 생성한다")
	void shouldCreateConnectionForValidUrl() throws IOException {
		// given
		String url = "https://example.com";

		// when
		URLConnection connection = factory.createConnection(url);

		// then
		assertThat(connection).isNotNull();
		assertThat(connection).isInstanceOf(HttpURLConnection.class);
	}

	@Test
	@DisplayName("createConnection: 잘못된 형식의 URL인 경우 MalformedURLException이 발생한다")
	void shouldThrowExceptionForInvalidUrlFormat() {
		// given
		String invalidUrl = "not-a-url";

		// when & then
		assertThatThrownBy(() -> factory.createConnection(invalidUrl))
			.isInstanceOf(MalformedURLException.class);
	}

	@Test
	@DisplayName("openStream: 연결 시 타임아웃(5000ms)을 설정하고 스트림을 반환한다")
	void shouldSetTimeoutsAndReturnStream() throws IOException {
		// given
		String url = "https://example.com/image.jpg";
		InputStream expectedStream = new ByteArrayInputStream("test-data".getBytes());

		doReturn(mockConnection).when(factory).createConnection(url);
		given(mockConnection.getInputStream()).willReturn(expectedStream);

		// when
		InputStream resultStream = factory.openStream(url);

		// then
		assertThat(resultStream).isEqualTo(expectedStream);

		verify(mockConnection).setConnectTimeout(5000);
		verify(mockConnection).setReadTimeout(5000);
		verify(mockConnection).getInputStream();
	}
}
