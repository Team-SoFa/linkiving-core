package com.sofa.linkiving.global.util;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HashidsUtils {
	private final Hashids hashids;

	public HashidsUtils(
		@Value("${hashids.salt}") String salt,
		@Value("${hashids.min-length:8}") int minLength) {
		this.hashids = new Hashids(salt, minLength);
	}

	public String encode(Long id) {
		if (id == null) {
			return null;
		}

		return hashids.encode(id);
	}

	public Long decode(String hash) {
		if (hash == null || hash.isBlank()) {
			return null;
		}

		long[] decoded = hashids.decode(hash);

		if (decoded.length == 0) {
			throw new IllegalArgumentException("유효하지 않은 식별자입니다.");
		}
		return decoded[0];
	}
}
