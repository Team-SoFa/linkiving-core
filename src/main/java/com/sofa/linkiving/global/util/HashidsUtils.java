package com.sofa.linkiving.global.util;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.global.error.code.CommonErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

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
			throw new BusinessException(CommonErrorCode.INVALID_IDENTIFIER);
		}
		return decoded[0];
	}
}
