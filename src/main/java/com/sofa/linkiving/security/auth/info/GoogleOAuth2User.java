package com.sofa.linkiving.security.auth.info;

import java.util.Map;

public record GoogleOAuth2User(
	Map<String, Object> attributes,
	String name,
	String email,
	String picture
) {
	public GoogleOAuth2User(Map<String, Object> attributes) {
		this(
			attributes,
			(String)attributes.get("name"),
			(String)attributes.get("email"),
			(String)attributes.get("picture")
		);
	}
}
