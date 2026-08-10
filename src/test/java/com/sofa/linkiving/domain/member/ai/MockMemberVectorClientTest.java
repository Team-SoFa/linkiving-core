package com.sofa.linkiving.domain.member.ai;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class MockMemberVectorClientTest {

	@Test
	void shouldAcceptVectorDeletion() {
		MockMemberVectorClient client = new MockMemberVectorClient();

		assertThatCode(() -> {
			client.validateConfiguration();
			client.deleteAll(1L);
		}).doesNotThrowAnyException();
	}
}
