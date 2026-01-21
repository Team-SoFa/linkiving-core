package com.sofa.linkiving.infra.s3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.stereotype.Component;

@Component
public class DefaultUrlConnectionFactory implements UrlConnectionFactory {

	@Override
	public URLConnection createConnection(String url) throws IOException {
		return new URL(url).openConnection();
	}

	@Override
	public InputStream openStream(String url) throws IOException {
		URLConnection connection = createConnection(url);
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(5000);
		return connection.getInputStream();
	}
}
