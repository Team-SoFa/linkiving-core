package com.sofa.linkiving.infra.s3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public interface UrlConnectionFactory {
	URLConnection createConnection(String url) throws IOException;

	InputStream openStream(String url) throws IOException;
}
