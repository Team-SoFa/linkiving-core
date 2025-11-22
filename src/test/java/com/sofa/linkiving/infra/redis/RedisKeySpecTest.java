package com.sofa.linkiving.infra.redis;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RedisKeySpecTest {

	@Test
	@DisplayName("숫자 코드 컬럼을 통해 Enum을 저장·조회")
	void shouldBuildKeyWithPrefixAndParts() {
		// given
		RedisKeySpec spec = new RedisKeySpec("rt", Duration.ofDays(180));

		// when
		String k1 = spec.key();
		String k2 = spec.key("user", "123"); // rt:user:123

		// then
		assertThat(k1).isEqualTo("rt");
		assertThat(k2).isEqualTo("rt:user:123");
	}
}
