package com.sofa.linkiving.infra.s3;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.stereotype.Component;

@Component
public class DefaultUrlConnectionFactory implements UrlConnectionFactory {

	@Override
	public URLConnection createConnection(String url) throws IOException {
		return new URL(url).openConnection();
	}
}
