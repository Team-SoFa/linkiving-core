package com.sofa.linkiving.infra.s3;

import java.io.IOException;
import java.net.URLConnection;

public interface UrlConnectionFactory {
	URLConnection createConnection(String url) throws IOException;
}
